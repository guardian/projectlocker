package controllers
import java.sql.Timestamp

import models.{FileEntry, FileEntryRow, StorageEntry, StorageEntryRow}
import org.joda.time.DateTime
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads.jodaDateReads
import play.api.libs.json.Writes.jodaDateWrites
import play.api.libs.json._
import play.api.mvc.{Action, BodyParsers, Controller, Request}
import slick.driver.JdbcProfile
import slick.lifted.TableQuery

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global


trait GenericDatabaseObjectController[M] extends Controller {
  def validate(request:Request[JsValue]):JsResult[M]

  def selectall:Future[Try[Seq[M]]]
  def selectid(requestedId: Int):Future[Try[Seq[M]]]

  def insert(entry: M):Future[Any]
  def jstranslate(result:Seq[M]):Json.JsValueWrapper
  def jstranslate(result:M):Json.JsValueWrapper

  def list = Action.async {
    selectall.map({
      case Success(result)=>Ok(Json.obj("status"->"ok","result"->this.jstranslate(result)))
      case Failure(error)=>InternalServerError(Json.obj("status"->"error","detail"->error.toString))
    })
  }

  def create = Action.async(BodyParsers.parse.json) { request =>
    this.validate(request).fold(
      errors => {
        Future(BadRequest(Json.obj("status"->"error","detail"->JsError.toJson(errors))))
      },
      storageEntry => {
        this.insert(storageEntry).map({
          case Success(result)=>Ok(Json.obj("status" -> "ok", "detail" -> "added", "id" -> result.asInstanceOf[Int]))
          case Failure(error)=>InternalServerError(Json.obj("status"->"error", "detail"->error.toString))
        }
        )
      }
    )
  }

  def getitem(requestedId: Int) = Action.async {
    selectid(requestedId).map({
      case Success(result)=>
        if(result.isEmpty)
         NotFound("")
        else
          Ok(Json.obj("status"->"ok","result"->this.jstranslate(result.head)))
      case Failure(error)=>InternalServerError(Json.obj("status"->"error","detail"->error.toString))
    })
  }

  def update(id: Int) = Action.async(BodyParsers.parse.json) { request =>
    this.validate(request).fold(
      errors=>Future(BadRequest(Json.obj("status"->"error","detail"->JsError.toJson(errors)))),
      StorageEntry=>Future(Ok(""))
    )
  }

  def delete(id: Int) = Action.async(BodyParsers.parse.json) { request =>
    Future(Ok(""))
  }
}
