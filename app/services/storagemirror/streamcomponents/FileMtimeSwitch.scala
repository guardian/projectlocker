package services.storagemirror.streamcomponents

import java.time.{Instant, ZoneId, ZonedDateTime}

import akka.stream.{Attributes, Inlet, Materializer, Outlet, UniformFanOutShape}
import akka.stream.stage.{AbstractInHandler, AbstractOutHandler, GraphStage, GraphStageLogic}
import models.{FileEntry, StorageEntry}
import org.slf4j.LoggerFactory

/**
  * pushes the given file to "yes" if the filesystem mtime is later than the database mtime (i.e. the replica must be updated)
  * @param storageRef [[StorageEntry]]
  * @param mat
  */
class FileMtimeSwitch (storageRef:StorageEntry)(implicit mat:Materializer) extends GraphStage[UniformFanOutShape[FileEntry, FileEntry]]{
  private final val in:Inlet[FileEntry] = Inlet.create("FileMtimeSwitch.in")
  private val yes:Outlet[FileEntry] = Outlet.create("FileMtimeSwitch.yes")
  private val no:Outlet[FileEntry] = Outlet.create("FileMtimeSwitch.no")

  override def shape: UniformFanOutShape[FileEntry, FileEntry] = new UniformFanOutShape[FileEntry, FileEntry](in, Array(yes, no))

  private val maybeStorageDriver = storageRef.getStorageDriver

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    private val logger = LoggerFactory.getLogger(getClass)

    setHandler(in, new AbstractInHandler {
      override def onPush(): Unit = {
        val elemToCheck = grab(in)

        maybeStorageDriver match {
          case None=>
            logger.error(s"Storage ${storageRef.repr} has no driver associated, can't perform any scan!")
            failStage(new RuntimeException("No driver associated with storage"))
          case Some(storageDriver)=>
            val meta = storageDriver.getMetadata(storageRef.fullPathFor(elemToCheck.filepath), elemToCheck.version)

            meta.get('lastModified) match {
              case None=>
                logger.error(s"File $elemToCheck has no modified time on disk! This may indicate a storage driver bug")
                failStage(new RuntimeException("File has no modified time on disk"))
              case Some(mTimeString)=>
                logger.debug(s"mTimeString for ${elemToCheck.filepath} is $mTimeString")

                try {
                  //handle potentially confusing cases of epoch seconds/milliseconds timestamps
                  val timeInstant = if(mTimeString.length<12){
                    Instant.ofEpochSecond(mTimeString.toLong)
                  } else {
                    Instant.ofEpochMilli(mTimeString.toLong)
                  }

                  val filesystemMtime = ZonedDateTime.ofInstant(timeInstant, ZoneId.systemDefault())
                  logger.debug(s"filesystem mTime for ${elemToCheck.filepath} is $filesystemMtime")
                  val databaseMtime = elemToCheck.mtime.toInstant.atZone(ZoneId.systemDefault())
                  logger.debug(s"database mTime for ${elemToCheck.filepath} is $databaseMtime")

                  if (filesystemMtime.isAfter(databaseMtime)) {
                    push(yes, elemToCheck)
                  } else {
                    push(no, elemToCheck)
                  }
                } catch {
                  case err:Throwable=>
                    logger.error(s"Could not get mtime for $elemToCheck: ", err)
                    failStage(err)
                }
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
