package helpers
import java.sql.Timestamp
import java.time.LocalDateTime

import com.google.inject.ImplementedBy
import models.{FileEntry, ProjectEntry, ProjectRequestFull}
import play.api.Logger

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

@ImplementedBy(classOf[ProjectCreateHelperImpl])
trait ProjectCreateHelper {
  def create(rq:ProjectRequestFull,createTime:Option[LocalDateTime])
            (implicit db: slick.driver.JdbcProfile#Backend#Database):Future[Try[ProjectEntry]]
}
