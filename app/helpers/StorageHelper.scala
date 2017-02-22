package helpers
import scala.concurrent.{ExecutionContext, Future}
import models.{StorageEntry, StorageEntryRow}
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.PostgresDriver.api._
import akka.event.{Logging, LoggingAdapter}
import scala.util.{Failure, Success, Try}

class TooManyRecordsError (message:String) extends RuntimeException
{
  override def getMessage = message

  override def toString = message
}

object StorageHelper {
  def defaultStorage(implicit dbConfig:DatabaseConfig[JdbcProfile], log:LoggingAdapter, ec: ExecutionContext):Future[Try[StorageEntry]] = {
    dbConfig.db.run(
      TableQuery[StorageEntryRow].filter(_.default === true).result.asTry
    ).map({
      case Success(result)=>
        if(result.length>1){
          Failure(new TooManyRecordsError("There should only be one storage tagged as the default"))
        } else {
          Success(result.head)
        }
      case Failure(error)=>
        log.error(error.toString)
        log.error(error.getStackTraceString)
        Failure(error)
    })
  }
}
