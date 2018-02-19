package helpers
import java.sql.Timestamp
import java.time.LocalDateTime

import com.google.inject.ImplementedBy
import models.{FileEntry, ProjectEntry, ProjectRequestFull}
import play.api.Logger

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * This protocol describes the interface of the ProjectCreateHelper class, for the purposes of DI
  */
@ImplementedBy(classOf[ProjectCreateHelperImpl])
trait ProjectCreateHelper {
  /**
    * See documentation in [[ProjectCreateHelperImpl]]
    * @param rq [[ProjectRequestFull]] object representing the project request
    * @param createTime optional [[LocalDateTime]] as the create time.  If None is provided then current date/time is used
    * @param db implicitly provided [[slick.driver.JdbcProfile#Backend#Database]]
    * @return a [[Try]] containing a saved [[models.ProjectEntry]] object if successful, wrapped in a  [[Future]]
    */
  def create(rq:ProjectRequestFull,createTime:Option[LocalDateTime])
            (implicit db: slick.driver.JdbcProfile#Backend#Database):Future[Try[ProjectEntry]]
}
