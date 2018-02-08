package controllers

import javax.inject.{Inject, Singleton}

import models._
import play.api.Configuration
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.JsValue
import play.api.mvc.{Action, BodyParsers, Request}
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.PostgresDriver.api._
import play.api.libs.json.{JsError, Json}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by localhome on 17/01/2017.
  */
@Singleton
class ProjectEntryController @Inject() (config: Configuration, dbConfigProvider: DatabaseConfigProvider)
  extends GenericDatabaseObjectController[ProjectEntry] with ProjectEntrySerializer with ProjectRequestSerializer
{
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  override def deleteid(requestedId: Int) = dbConfig.db.run(
    TableQuery[ProjectEntryRow].filter(_.id === requestedId).delete.asTry
  )

  override def selectid(requestedId: Int) = dbConfig.db.run(
    TableQuery[ProjectEntryRow].filter(_.id === requestedId).result.asTry
  )

  override def selectall = dbConfig.db.run(
    TableQuery[ProjectEntryRow].result.asTry //simple select *
  )

  override def jstranslate(result: Seq[ProjectEntry]) = result
  override def jstranslate(result: ProjectEntry) = result  //implicit translation should handle this

  override def insert(entry: ProjectEntry) = dbConfig.db.run(
    (TableQuery[ProjectEntryRow] returning TableQuery[ProjectEntryRow].map(_.id) += entry).asTry
  )

  override def validate(request:Request[JsValue]) = request.body.validate[ProjectEntry]

  override def create = Action.async(BodyParsers.parse.json) { request =>
    implicit val db = dbConfig.db

    request.body.validate[ProjectRequest].fold(
      errors=>
        Future(BadRequest(Json.obj("status"->"error","detail"->JsError.toJson(errors)))),
      projectRequest=> {
        val fullRequestFuture=projectRequest.hydrate
        fullRequestFuture.map({
          case None=>
            BadRequest(Json.obj("status"->"error","detail"->"Invalid template or storage ID"))
          case Some(rq)=>

            Ok("")
        })

      })
  }

}
