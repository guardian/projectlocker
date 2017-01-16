package controllers

import com.google.inject.Inject
import models.{StorageEntry, StorageEntryRow}
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

class StoragesController @Inject() (configuration: Configuration, dbConfigProvider: DatabaseConfigProvider) extends GenericDatabaseObjectController {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  /*https://www.playframework.com/documentation/2.5.x/ScalaJson*/
  implicit val storageWrites:Writes[StorageEntry] = (
    (JsPath \ "id").writeNullable[Int] and
      (JsPath \ "rootpath").writeNullable[String] and
      (JsPath \ "storageType").write[String] and
      (JsPath \ "user").writeNullable[String] and
      (JsPath \ "password").writeNullable[String] and
      (JsPath \ "host").writeNullable[String] and
      (JsPath \ "port").writeNullable[Int]
  )(unlift(StorageEntry.unapply))

  implicit val storageReads:Reads[StorageEntry] = (
    (JsPath \ "id").readNullable[Int] and
      (JsPath \ "rootpath").readNullable[String] and
      (JsPath \ "storageType").read[String] and
      (JsPath \ "user").readNullable[String] and
      (JsPath \ "password").readNullable[String] and
      (JsPath \ "host").readNullable[String] and
      (JsPath \ "port").readNullable[Int]
    )(StorageEntry.apply _)

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
          (TableQuery[StorageEntryRow] += StorageEntry).asTry
        ).map({
          case Success(result)=>Ok(Json.obj("status" -> "ok", "detail" -> "added", "result" -> result))
          case Failure(error)=>InternalServerError(Json.obj("status"->"error", "detail"->error.toString))
        }
        )
      }
    )
  }

  override def update(id: Int) = Action.async(BodyParsers.parse.json) { request =>
    request.body.validate[StorageEntry].fold(
      errors=>Future(BadRequest(Json.obj("status"->"error","detail"->JsError.toJson(errors)))),
      FileEntry=>Future(Ok(""))
    )
  }

  override def delete(id: Int) = Action.async(BodyParsers.parse.json) { request =>
    Future(Ok(""))
  }
}
