package services.storagemirror

import akka.actor.{Actor, ActorSystem}
import akka.stream.{ClosedShape, FlowShape, Materializer}
import akka.stream.scaladsl.{GraphDSL, Sink}
import javax.inject.{Inject, Singleton}
import models.{FileEntry, FileEntryRow, StorageEntry}
import play.api.db.slick.DatabaseConfigProvider
import services.storagemirror.MirrorScanActor.{MirrorScanStorage, TimedTrigger}
import services.storagemirror.streamcomponents._
import slick.jdbc.PostgresProfile
import slick.lifted.TableQuery
import akka.stream.alpakka.slick.scaladsl._
import akka.stream.scaladsl._
import slick.jdbc.GetResult
import slick.jdbc.PostgresProfile.api._

object MirrorScanActor {
  trait MSMsg

  case class MirrorScanStorage(storageRef:StorageEntry)
  case object TimedTrigger
}

@Singleton
class MirrorScanActor @Inject() (dbConfigProvider:DatabaseConfigProvider)(implicit system:ActorSystem, mat:Materializer) extends Actor {
  private implicit val db = dbConfigProvider.get[PostgresProfile].db

  implicit val session = SlickSession.forConfig("slick-h2")
  system.registerOnTermination(session.close())

  def buildCopystream(replicaCopyFactory:CreateReplicaCopy, fileContentFactory:CopyFileContent) = GraphDSL.create() { implicit builder=>
    import akka.stream.scaladsl.GraphDSL.Implicits._

    val createReplica = builder.add(replicaCopyFactory)
    val copyContent = builder.add(fileContentFactory)

    createReplica ~> copyContent
    FlowShape(createReplica.in, copyContent.out)
  }

  def buildStream(sourceStorage:StorageEntry, destStorage:StorageEntry, paralellism:Int, dbParalellism:Int=4) = {
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

  override def receive: Receive = {
    case TimedTrigger=>
      //search database for storages that have replicas set

    case MirrorScanStorage(storageRef)=>

  }
}
