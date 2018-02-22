package helpers

import models.{FileEntry, ProjectEntry, ProjectRequestFull}
import java.sql.Timestamp

import scala.concurrent.Future
import java.time.LocalDateTime
import javax.inject.Singleton

import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

@Singleton
class ProjectCreateHelperImpl extends ProjectCreateHelper {
  protected val storageHelper:StorageHelper = new StorageHelper
  val logger: Logger = Logger(this.getClass)

  /**
    * Logic to create a project.  This runs asynchronously, taking in a project request in the form of a [[models.ProjectRequestFull]]
    * and copying the requested template to the final destination
    * @param rq [[ProjectRequestFull]] object representing the project request
    * @param createTime optional [[LocalDateTime]] as the create time.  If None is provided then current date/time is used
    * @param db implicitly provided [[slick.driver.JdbcProfile#Backend#Database]]
    * @return a [[Try]] containing a saved [[models.ProjectEntry]] object if successful, wrapped in a  [[Future]]
    */
  def create(rq:ProjectRequestFull,createTime:Option[LocalDateTime])(implicit db: slick.driver.JdbcProfile#Backend#Database):Future[Try[ProjectEntry]] = {
    logger.info(s"Creating project from $rq")
    rq.destinationStorage.getStorageDriver match {
      case None=>
        Future(Failure(new RuntimeException(s"Storage ${rq.destinationStorage.id} does not have any storage driver configured")))
      case Some(storageDriver)=>
        logger.info(s"Got storage driver: $storageDriver")

        val recordTimestamp = Timestamp.valueOf(createTime.getOrElse(LocalDateTime.now()))
        val destFileEntry = FileEntry(None,rq.filename,rq.destinationStorage.id.get,"system",1,
          recordTimestamp,recordTimestamp,recordTimestamp, hasContent = false, hasLink = false)

        destFileEntry.save flatMap {
          case Success(savedFileEntry)=>
            val fileCopyFuture=rq.projectTemplate.file.flatMap(sourceFileEntry=>{
              logger.info(s"Copying from file $sourceFileEntry to $savedFileEntry")
              storageHelper.copyFile(sourceFileEntry, savedFileEntry)
            })

            fileCopyFuture.flatMap({
              case Left(error)=>
                logger.error(s"File copy failed: ${error.toString}")
                Future(Failure(new RuntimeException(error.mkString("\n"))))
              case Right(writtenFile)=>
                logger.info(s"Creating new project entry from $writtenFile")
                val result = ProjectEntry.createFromFile(writtenFile, rq.projectTemplate, rq.title, createTime,rq.user)
                logger.info("Done")
                result
            })
          case Failure(error)=>
            logger.error("Unable to save destination file entry to database", error)
            Future(Failure(error))
        }
    }
  }
}
