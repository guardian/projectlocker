package controllers

import com.google.inject.Inject
import play.api.Configuration
import play.api.db.slick.DatabaseConfigProvider
import play.api.mvc.{Action, Controller}
import slick.driver.JdbcProfile

import scala.util.{Failure, Success}
// Use H2Driver to connect to an H2 database
import slick.driver.H2Driver.api._


import scala.concurrent.Future
import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.sql.Timestamp

import scala.concurrent.ExecutionContext.Implicits.global
import models.{FileEntry, FileEntryRow}
import play.api.mvc.BodyParsers

/**
  * Created by localhome on 14/01/2017.
  */

class Files @Inject() (configuration: Configuration, dbConfigProvider: DatabaseConfigProvider) extends Controller {
  implicit val timestampReads: Reads[Timestamp] = JsPath.read[String].map(Timestamp.valueOf _)

  /*https://www.playframework.com/documentation/2.5.x/ScalaJson*/
  implicit val fileWrites: Writes[FileEntry] = (
    (JsPath \ "id").writeNullable[Int] and
      (JsPath \ "filepath").write[String] and
      (JsPath \ "storageType").write[String] and
      (JsPath \ "user").write[String] and
      (JsPath \ "isDir").write[Boolean] and
      (JsPath \ "ctime").write[Timestamp] and
      (JsPath \ "mtime").write[Timestamp] and
      (JsPath \ "atime").write[Timestamp]
    )(unlift(FileEntry.unapply))

  implicit val fileReads: Reads[FileEntry] = (
    (JsPath \ "id").readNullable[Int] and
      (JsPath \ "filepath").read[String] and
      (JsPath \ "storageType").read[String] and
      (JsPath \ "user").read[String] and
      (JsPath \ "isDir").read[Boolean] and
      (JsPath \ "ctime").read[Timestamp] and
      (JsPath \ "mtime").read[Timestamp] and
      (JsPath \ "atime").read[Timestamp]
    )(FileEntry.apply _)

  val dbConfig = dbConfigProvider.get[JdbcProfile]

  def list = Action.async {
    Future(Ok(""))
  }

  def create = Action.async(BodyParsers.parse.json) { request =>
    request.body.validate[FileEntry].fold(
      errors => {
        Future(BadRequest(Json.obj("status"->"error","detail"->JsError.toJson(errors))))
      },
      FileEntry => {
          dbConfig.db.run(
            (TableQuery[FileEntryRow] += FileEntry).asTry
          ).map({
            case Success(result)=>Ok(Json.obj("status" -> "ok", "detail" -> "added", "id" -> "?"))
            case Failure(error)=>InternalServerError(Json.obj("status"->"error", "detail"->error.toString))
          }
          )
      }
    )
  }

  def update(id: Int) = Action.async {
    Future(Ok(""))
  }

  def delete(id: Int) = Action.async {
    Future(Ok(""))
  }
}
