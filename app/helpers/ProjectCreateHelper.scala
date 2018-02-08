package helpers

import models.{FileEntry, ProjectEntry, ProjectRequestFull}
import java.sql.Timestamp
import scala.concurrent.Future
import java.time.LocalDateTime

object ProjectCreateHelper {
  def create(rq:ProjectRequestFull)(implicit db: slick.driver.JdbcProfile#Backend#Database):Future[Either[String,ProjectEntry]] = {
    rq.destinationStorage.getStorageDriver match {
      case None=>Future(Left(s"Storage ${rq.destinationStorage.id} does not have any storage driver configured"))
      case Some(storageDriver)=>
        val destFileEntry = FileEntry(None,rq.filename,rq.destinationStorage.id.get,"system",1,
          Timestamp.valueOf(LocalDateTime.now()),Timestamp.valueOf(LocalDateTime.now()),Timestamp.valueOf(LocalDateTime.now()),
          hasContent = false, hasLink = false)

        rq.projectTemplate.file.map(fileEntry=>{
          fileEntry.getFullPath.map(sourcePath=>{
            storageDriver.copy
          })
        })

        Right(ProjectEntry)
    }
  }
}
