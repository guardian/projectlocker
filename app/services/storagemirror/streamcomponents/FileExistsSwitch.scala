package services.storagemirror.streamcomponents

import akka.stream.{Attributes, Inlet, Materializer, Outlet, UniformFanOutShape}
import akka.stream.stage.{AbstractInHandler, AbstractOutHandler, GraphStage, GraphStageLogic}
import models.{FileEntry, StorageEntry}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Routes incoming [[FileEntry]] records to the "yes" port if the exist on the storage or "no" if they do not
  * @param storageRef [[StorageEntry]] instance describing the storage to perform lookups on
  * @param mat implicitly provided akka.stream.Materializer
  */
class FileExistsSwitch (storageRef:StorageEntry) (implicit mat:Materializer) extends GraphStage[UniformFanOutShape[FileEntry, FileEntry]] {
  private val in:Inlet[FileEntry] = Inlet.create("FileExistsSwitch.in")
  private val yes:Outlet[FileEntry] = Outlet.create("FileExistsSwitch.yes")
  private val no:Outlet[FileEntry] = Outlet.create("FileExistsSwitch.no")

  override def shape: UniformFanOutShape[FileEntry, FileEntry] = new UniformFanOutShape[FileEntry, FileEntry](in, Array(yes, no))

  private val maybeStorageDriver = storageRef.getStorageDriver

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    private val logger = LoggerFactory.getLogger(getClass)

    setHandler(in, new AbstractInHandler {
      override def onPush(): Unit = {
        val entryToTest = grab(in)

        maybeStorageDriver match {
          case None=>
            logger.error(s"Storage ${storageRef.repr} has no driver associated, can't perform any scan!")
            failStage(new RuntimeException("No driver associated with storage"))
          case Some(driver)=>
            if(driver.pathExists(storageRef.fullPathFor(entryToTest.filepath), entryToTest.version)){
              push(yes, entryToTest)
            } else {
              push(no, entryToTest)
            }
        }
      }
    })

    setHandler(yes, new AbstractOutHandler {
      override def onPull(): Unit = if(!hasBeenPulled(in)) pull(in)
    })

    setHandler(no, new AbstractOutHandler {
      override def onPull(): Unit = if(!hasBeenPulled(in)) pull(in)
    })
  }
}
