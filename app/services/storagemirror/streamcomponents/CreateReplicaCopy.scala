package services.storagemirror.streamcomponents

import akka.actor.ActorSystem
import akka.stream.{Attributes, FlowShape, Inlet, Materializer, Outlet}
import akka.stream.stage.{AbstractInHandler, AbstractOutHandler, GraphStage, GraphStageLogic}
import models.{FileEntry, StorageEntry}
import org.slf4j.LoggerFactory

import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success}
import scala.concurrent.duration._

/**
  * creates or re-uses a [[FileEntry]] object to use as the copy destination for an incoming [[FileEntry]]
  * yields out [[ReplicaJob]] models that contain the source and destination [[FileEntry]] objects
  * @param destStorage [[StorageEntry]] object representing the storage to perform the copy to
  * @param db implicitly provided database object to perform lookups
  */
class CreateReplicaCopy (destStorage: StorageEntry)(implicit db:slick.jdbc.PostgresProfile#Backend#Database, actorSystem: ActorSystem) extends GraphStage[FlowShape[FileEntry, ReplicaJob]] {
  private final val in:Inlet[FileEntry] = Inlet.create("CreateReplicaCopy.in")
  private final val out:Outlet[ReplicaJob] = Outlet.create("CreateReplicaCopy.out")

  override def shape: FlowShape[FileEntry, ReplicaJob] = FlowShape.of(in, out)

  private implicit val ec:ExecutionContext = actorSystem.dispatcher

  def createNewFileEntry(sourceEntry:FileEntry, versionOverride:Option[Int]=None):FileEntry = versionOverride match {
    case None=>sourceEntry.copy(id=destStorage.id, mirrorParent=sourceEntry.id)
    case Some(actualOverride)=>sourceEntry.copy(id=destStorage.id, mirrorParent=sourceEntry.id, version=actualOverride)
  }

  //calls to static methods are factored out here to make testing easier
  protected def findExistingSingle(filePath:String, destStorageId:Int, version:Int) = FileEntry.entryFor(filePath, destStorageId, version)

  protected def findAllVersions(filePath:String, storageId:Int) = FileEntry.allVersionsFor(filePath, storageId)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    private val logger = LoggerFactory.getLogger(getClass)

    setHandler(in, new AbstractInHandler {
      override def onPush(): Unit = {
        val sourceEntry = grab(in)

        val failedCb = createAsyncCallback[Throwable](err=> {
          logger.error("Failed to create replica copy: ", err)
          failStage(err)
        })

        val successCb = createAsyncCallback[ReplicaJob](job=> {
          println(s"Got $job")
          push(out, job)
        })

        /*get our destination entry.  This could be obtained one of three ways:
          1. Nothing exists. If so create a new one at the same version as the source (createNewFileEntry above)
          2. We have an existing file at the destination, and versioning is NOT enabled. If so, overwrite it.
          3. We have an existing file at the destination, and versioning IS enabled. If so, we create a new one with a version one more than the highest version number
          already present on the source.
        */
        val destEntryFut = if(destStorage.supportsVersions){
          //create a new file version
          findAllVersions(sourceEntry.filepath, destStorage.id.get).map(entrySeq=>
            if(entrySeq.isEmpty){
              createNewFileEntry(sourceEntry)
            } else {
              createNewFileEntry(sourceEntry, versionOverride=Some(entrySeq.maxBy(_.version).version+1))
            })
        } else {
          //overwrite the existing version
          findExistingSingle(sourceEntry.filepath, destStorage.id.get, sourceEntry.version).map({
            case Failure(err)=>
              logger.error(s"Could look up pre-existing file for $sourceEntry on storage ${destStorage.id}: ", err)
              failedCb.invoke(err)
              throw err
            case Success(entrySeq)=>
              if(entrySeq.isEmpty){
                createNewFileEntry(sourceEntry)
              } else if(entrySeq.length>1){
                logger.error(s"Found multiple files for name ${sourceEntry.filepath} on storage ${destStorage.id} when versions not enabled!")
                val err = new RuntimeException("Multiple files found when versions not enabled")
                failedCb.invoke(err)
                throw err
              } else {
                entrySeq.head
              }
          })
        }

        //TODO: for some reason the async processing below causes the stage to terminate early, i.e. before the successCb is invoked.
        val updatedEntry = Await.result(destEntryFut.flatMap(_.save), 60 seconds)
        val replicaJob = ReplicaJob(sourceEntry, updatedEntry, None)
        push(out, replicaJob)

//        destEntryFut.flatMap(unsavedDestEntry=>{
//          unsavedDestEntry.save.map(updatedEntry=>{
//            val replicaJob = ReplicaJob(sourceEntry, updatedEntry, None)
//            successCb.invokeWithFeedback(replicaJob).map(_=>
//              println("successcb completed")
//            ).recover({
//              case err:Throwable=>
//                println("could not invoke callback:")
//                println(err)
//            })
//          }).recover({
//            case err:Throwable=>
//              logger.error(s"Could not create replicate destination for $sourceEntry: ", err)
//              failedCb.invoke(err)
//          })
//        })
      }
    })

    setHandler(out, new AbstractOutHandler {
      override def onPull(): Unit = pull(in)
    })
  }
}
