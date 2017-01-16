package controllers

import com.google.inject.Inject
import models.{StorageEntry, StorageEntryRow, StorageSerializer}
import play.api.Configuration
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{Action, BodyParsers}
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.PostgresDriver.api._

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

class StoragesController @Inject()
    (configuration: Configuration, dbConfigProvider: DatabaseConfigProvider)
    extends GenericDatabaseObjectController with StorageSerializer {
  val dbConfig = dbConfigProvider.get[JdbcProfile]


  override def list = Action.async {
    dbConfig.db.run(
      TableQuery[StorageEntryRow].result.asTry //simple select *
    ).map({
      case Success(result)=>Ok(Json.obj("status"->"ok","result"->result))
      case Failure(error)=>InternalServerError(Json.obj("status"->"error","detail"->error.toString))
    })
  }

  override def create = Action.async(BodyParsers.parse.json) { request =>
    request.body.validate[StorageEntry].fold(
      errors => {
        Future(BadRequest(Json.obj("status"->"error","detail"->JsError.toJson(errors))))
      },
      StorageEntry => {
        dbConfig.db.run(
          (TableQuery[StorageEntryRow] returning TableQuery[StorageEntryRow].map(_.id) += StorageEntry).asTry
        ).map({
          case Success(result)=>Ok(Json.obj("status" -> "ok", "detail" -> "added", "id" -> result))
          case Failure(error)=>InternalServerError(Json.obj("status"->"error", "detail"->error.toString))
        }
        )
      }
    )
  }

  override def update(id: Int) = Action.async(BodyParsers.parse.json) { request =>
    request.body.validate[StorageEntry].fold(
      errors=>Future(BadRequest(Json.obj("status"->"error","detail"->JsError.toJson(errors)))),
      StorageEntry=>Future(Ok(""))
    )
  }

  override def delete(id: Int) = Action.async(BodyParsers.parse.json) { request =>
    Future(Ok(""))
  }
}
