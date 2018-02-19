package controllers

import javax.inject.{Inject, Singleton}

import helpers.ProjectCreateHelper
import models._
import play.api.{Configuration, Logger}
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.JsValue
import play.api.mvc.{Action, BodyParsers, Controller, Request}
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.PostgresDriver.api._
import play.api.libs.json.{JsError, Json}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

/**
  * Created by localhome on 17/01/2017.
  */
@Singleton
class ProjectEntryController @Inject() (config: Configuration, dbConfigProvider: DatabaseConfigProvider, projectHelper:ProjectCreateHelper)
  extends Controller with ProjectEntrySerializer with ProjectRequestSerializer
{
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  /*
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
*/

  def create = Action.async(BodyParsers.parse.json) { request =>
    implicit val db = dbConfig.db

    request.body.validate[ProjectRequest].fold(
      errors=>
        Future(BadRequest(Json.obj("status"->"error","detail"->JsError.toJson(errors)))),
      projectRequest=> {
        val fullRequestFuture=projectRequest.hydrate
        fullRequestFuture.flatMap({
          case None=>
            Future(BadRequest(Json.obj("status"->"error","detail"->"Invalid template or storage ID")))
          case Some(rq)=>
            projectHelper.create(rq,None).map({
              case Failure(error)=>
                Logger.error("Could not create new project", error)
                InternalServerError(Json.obj("status"->"error","detail"->error.toString))
              case Success(projectEntry)=>
                Logger.error(s"Created new project: $projectEntry")
                Ok(Json.obj("status"->"ok","detail"->"created project", "projectId"->projectEntry.id.get))
            })
        })
      })
  }

}
