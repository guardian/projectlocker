package controllers

import com.google.inject.Inject
import models.{ProjectTemplateSerializer, ProjectTemplate, ProjectTemplateRow, StorageSerializer}
import play.api.Configuration
import play.api.mvc._
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json._
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.PostgresDriver.api._

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

class ProjectTemplateController @Inject() (config: Configuration, dbConfigProvider: DatabaseConfigProvider)
  extends GenericDatabaseObjectController with ProjectTemplateSerializer with StorageSerializer{

  val dbConfig = dbConfigProvider.get[JdbcProfile]

  override def list = Action.async {
    dbConfig.db.run(
      TableQuery[ProjectTemplateRow].result.asTry //simple select *
    ).map({
      case Success(result)=>Ok(Json.obj("status"->"ok","result"->result))
      case Failure(error)=>InternalServerError(Json.obj("status"->"error","detail"->error.toString))
    })
  }

  override def create = Action.async(BodyParsers.parse.json) { request =>
    request.body.validate[ProjectTemplate].fold(
      errors => {
        Future(BadRequest(Json.obj("status"->"error","detail"->JsError.toJson(errors))))
      },
      projectTemplate => {
        dbConfig.db.run(
          (TableQuery[ProjectTemplateRow] returning TableQuery[ProjectTemplateRow].map(_.id) += projectTemplate).asTry
        ).map({
          case Success(result)=>Ok(Json.obj("status" -> "ok", "detail" -> "added", "id" -> result))
          case Failure(error)=>InternalServerError(Json.obj("status"->"error", "detail"->error.toString))
        }
        )
      }
    )
  }

  override def update(id: Int) = Action.async(BodyParsers.parse.json) { request =>
    request.body.validate[ProjectTemplate].fold(
      errors=>Future(BadRequest(Json.obj("status"->"error","detail"->JsError.toJson(errors)))),
      StorageEntry=>Future(Ok(""))
    )
  }

  override def delete(id: Int) = Action.async(BodyParsers.parse.json) { request =>
    Future(Ok(""))
  }
}
