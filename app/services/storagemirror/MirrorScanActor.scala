package services.storagemirror

import akka.Done
import akka.actor.{Actor, ActorSystem}
import akka.stream.{ClosedShape, FlowShape, Materializer}
import akka.stream.scaladsl.{GraphDSL, Sink}
import javax.inject.{Inject, Singleton}
import models.{FileEntry, FileEntryRow, StorageEntry, StorageEntryHelper, StorageMirror}
import play.api.db.slick.DatabaseConfigProvider
import services.storagemirror.MirrorScanActor.{MirrorScanStorage, TimedTrigger}
import services.storagemirror.streamcomponents._
import slick.jdbc.PostgresProfile
import slick.lifted.TableQuery
import akka.stream.alpakka.slick.scaladsl._
import akka.stream.scaladsl._
import org.slf4j.LoggerFactory
import play.api.Configuration
import slick.jdbc.GetResult
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object MirrorScanActor {
  trait MSMsg

  case class MirrorScanStorage(storageRef:StorageEntry) extends MSMsg
  case object TimedTrigger extends MSMsg

  case object NoMirrorTargets extends MSMsg
}

@Singleton
class MirrorScanActor @Inject() (dbConfigProvider:DatabaseConfigProvider, config:Configuration)(implicit system:ActorSystem, mat:Materializer) extends Actor {
  import MirrorScanActor._

  private implicit val db = dbConfigProvider.get[PostgresProfile].db

  private val logger = LoggerFactory.getLogger(getClass)

  implicit val session = SlickSession.forConfig("slick.dbs.default")
  system.registerOnTermination(session.close())

  /**
    * this builds a substream for performing the actual copy function which is wired into the main graph obtained from buildStream()
    * the substream assumes the shape of a Flow, i.e. one input and one output; it receives [[FileEntry]] instances and outputs
    * more information in the form of a [[ReplicaJob]] instance
    * @param replicaCopyFactory instance of [[CreateReplicaCopy]]
    * @param fileContentFactory instance of [[CopyFileContent]]
    * @return the configured graph
    */
  protected def buildCopystream(replicaCopyFactory:CreateReplicaCopy, fileContentFactory:CopyFileContent) = GraphDSL.create() { implicit builder=>
    import akka.stream.scaladsl.GraphDSL.Implicits._

    val createReplica = builder.add(replicaCopyFactory)
    val copyContent = builder.add(fileContentFactory)

    createReplica ~> copyContent
    FlowShape(createReplica.in, copyContent.out)
  }

  /**
    * build a stream (as an Akka graph) for running a "mirror scan" for a given source and destination storage.
    * this is converted to a RunnableGraph and run from the main handler and is included here to make testing easier.
    * @param sourceStorage [[StorageEntry]] representing the storage to be scanned
    * @param destStorage [[StorageEntry]] representing the destination storage, to receive updates from the source
    * @param paralellism maximum number of parallel copy operations to maintain at once
    * @param dbParalellism maximum number of database operations to have ongoing at once (default 4)
    * @return a configured Graph that yields a Future[Done] to detect completion
    */
  protected def buildStream(sourceStorage:StorageEntry, destStorage:StorageEntry, paralellism:Int, dbParalellism:Int=4) = {
    val finalSinkFac = Sink.ignore
    val replicaCopyFactory = new CreateReplicaCopy(destStorage)
    val fileContentFactory = new CopyFileContent(sourceStorage, destStorage)

    val copyStreamFactory = buildCopystream(replicaCopyFactory, fileContentFactory)

    GraphDSL.create(finalSinkFac) {implicit builder=> finalSink=>
      import akka.stream.scaladsl.GraphDSL.Implicits._

      //scan entries from the FileEntry table whose storage matches our SourceStorage
      val src = builder.add(Slick.source(TableQuery[FileEntryRow].filter(_.storage===sourceStorage.id.get).filter(_.hasContent===true).result))
      //YES if file exists, NO otherwise
      val existSwitch = builder.add(new FileExistsSwitch(sourceStorage))
      //YES if file system mtime > database mtime, NO otherwise
      val mTimeSwitch = builder.add(new FileMtimeSwitch(sourceStorage))

      //parallel out the copying part
      val copyStreamBalancer = builder.add(Balance[FileEntry](paralellism))
      val copyStreamMerger = builder.add(Merge[ReplicaJob](paralellism))

      val fileEntryUnpacker = builder.add(new FileEntryUnpacker)

      val markLost = builder.add(new FileEntryMarkLost)

      //a standard Slick Sink that writes incoming FileEntries back to the database
      val writerMerge = builder.add(Merge[FileEntry](2))

      val stmt = (fileEntry:FileEntry)=>TableQuery[FileEntryRow].filter(_.id===fileEntry.id).update(fileEntry)
      val slickSinkFact = Slick.sink[FileEntry](stmt)
      val writerSink = builder.add(slickSinkFact)

      src ~> existSwitch
      existSwitch.out(0) ~> mTimeSwitch //file does exist, check mtime
      existSwitch.out(1) ~> markLost ~> writerMerge

      mTimeSwitch.out(0) ~> copyStreamBalancer
      mTimeSwitch.out(1) ~> finalSink //mtime matches, don't need to replicate

      for(i<-0 to paralellism){
        val copyStream = builder.add(copyStreamFactory)
        copyStreamBalancer.out(i) ~> copyStream ~> copyStreamMerger.in(i)
      }

      copyStreamMerger ~> fileEntryUnpacker ~> writerMerge

      writerMerge ~> writerSink
      ClosedShape
    }
  }

  /**
    * get configured mirror targets for the given source storage ID
    * this calls directly through to StorageMirror, it is done like this to make testing easier
    * @param storageId numeric ID of the source storage to query
    * @return a Future containing a sequence of StorageMirror objects
    */
  protected def mirrorTargetsForStorage(storageId:Int) = StorageMirror.mirrorTargetsForStorage(storageId)

  protected val ownRef = self

  override def receive: Receive = {
    case TimedTrigger=>
      //search database for storages that have replicas set

    case MirrorScanStorage(storageRef)=>
      val originalSender = sender()

      mirrorTargetsForStorage(storageRef.id.get).flatMap(mirrorTargets=>{
        if(mirrorTargets.isEmpty){
          logger.error(s"Could not perform mirror scan on storage ${storageRef.id.get} because it has no mirror targets set")
          originalSender ! NoMirrorTargets
          //each sequence entry is source (option storage ID), destination (option storage ID)
          Future(Seq((storageRef.id, None)))
        } else {
          val destStorageList = Future.sequence(mirrorTargets.map(tgt =>
            StorageEntryHelper.entryFor(tgt.mirrorTargetStorageId)
          )).map(_.collect({ case Some(strg) => strg }))

          val parallelism = config.getOptional[Int]("storagereplication.parallelism").getOrElse(1)

          val graphSeqFut = destStorageList.map(_.map(destStorage => (destStorage.id, storageRef.id, RunnableGraph.fromGraph(buildStream(storageRef, destStorage, parallelism)))))

          graphSeqFut.flatMap(destInfo => Future.sequence(destInfo.map(graphSourceAndDest=>{
            logger.info(s"Starting mirror scan from storageID ${graphSourceAndDest._2} to storageID ${graphSourceAndDest._1}")
            graphSourceAndDest._3.run().map(_=>{
              logger.info(s"Mirror scan from storageID ${graphSourceAndDest._2} to storageID ${graphSourceAndDest._1} completed.")
              (graphSourceAndDest._1, graphSourceAndDest._2)
            })
          })))
        }
      }).onComplete({
        case Success(results)=>
          val actualCompletions = results.map(_._2).collect({case Some(destStorageId)=>destStorageId})
          if(actualCompletions.isEmpty){
            logger.info(s"Completed with no scans")
          } else {
            logger.info(s"All mirror scans for source storageID ${storageRef.id} have completed")
            originalSender ! akka.actor.Status.Success
          }
        case Failure(err)=>
          logger.error(s"One or more mirror scans for source storageID ${storageRef.id} have failed: ", err)
          originalSender ! akka.actor.Status.Failure(err)
      })
  }
}
