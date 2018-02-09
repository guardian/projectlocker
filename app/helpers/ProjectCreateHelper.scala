package helpers

import models.{FileEntry, ProjectEntry, ProjectRequestFull}
import java.sql.Timestamp

import scala.concurrent.Future
import java.time.LocalDateTime

import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

class ProjectCreateHelper {
  protected val storageHelper:StorageHelper = new StorageHelper

  def create(rq:ProjectRequestFull,createTime:Option[LocalDateTime])(implicit db: slick.driver.JdbcProfile#Backend#Database):Future[Try[ProjectEntry]] = {
    Logger.info(s"Creating project from $rq")
    rq.destinationStorage.getStorageDriver match {
      case None=>
        Future(Failure(new RuntimeException(s"Storage ${rq.destinationStorage.id} does not have any storage driver configured")))
      case Some(storageDriver)=>
        Logger.info(s"Got storage driver: $storageDriver")

        val recordTimestamp = Timestamp.valueOf(createTime.getOrElse(LocalDateTime.now()))
        val destFileEntry = FileEntry(None,rq.filename,rq.destinationStorage.id.get,"system",1,
          recordTimestamp,recordTimestamp,recordTimestamp, hasContent = false, hasLink = false)

        destFileEntry.save flatMap {
          case Success(savedFileEntry)=>
            val fileCopyFuture=rq.projectTemplate.file.flatMap(sourceFileEntry=>{
              Logger.info(s"Copying from file $sourceFileEntry to $savedFileEntry")
              storageHelper.copyFile(sourceFileEntry, savedFileEntry)
            })

            fileCopyFuture.flatMap({
              case Left(error)=>
                Logger.error(s"File copy failed: ${error.toString}")
                Future(Failure(new RuntimeException(error.mkString("\n"))))
              case Right(writtenFile)=>
                Logger.info(s"Creating new project entry from $writtenFile")
                val result = ProjectEntry.createFromFile(writtenFile, rq.projectTemplate,createTime,rq.user)
                Logger.info("Done")
                result
            })
          case Failure(error)=>
            Logger.error("Unable to save destination file entry to database", error)
            Future(Failure(error))
        }
    }
  }
}
