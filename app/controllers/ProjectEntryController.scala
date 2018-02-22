package controllers

import javax.inject.{Inject, Singleton}
import auth.Security
import com.unboundid.ldap.sdk.LDAPConnectionPool
import helpers.ProjectCreateHelper
import models._
import play.api.cache.SyncCacheApi
import play.api.{Configuration, Logger}
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.JsValue
import play.api.mvc._
import slick.jdbc.JdbcProfile
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
class ProjectEntryController @Inject() (cc:ControllerComponents, config: Configuration,
                                        dbConfigProvider: DatabaseConfigProvider, projectHelper:ProjectCreateHelper,
                                        cacheImpl:SyncCacheApi)
  extends GenericDatabaseObjectController[ProjectEntry] with ProjectEntrySerializer with ProjectRequestSerializer with Security
{
  override implicit val cache:SyncCacheApi = cacheImpl

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

  /*this is pointless because of the override of [[create]] below, so it should not get called,
   but is needed to conform to the [[GenericDatabaseObjectController]] protocol*/
  override def insert(entry: ProjectEntry,uid:String) = Future(Failure(new RuntimeException("ProjectEntryController::insert should not have been called")))

  override def validate(request:Request[JsValue]) = request.body.validate[ProjectEntry]

  override def create = IsAuthenticatedAsync(BodyParsers.parse.json) {uid=>{ request =>
    implicit val db = dbConfig.db

    request.body.validate[ProjectRequest].fold(
      errors=>
        Future(BadRequest(Json.obj("status"->"error","detail"->JsError.toJson(errors)))),
      projectRequest=> {
        val fullRequestFuture=projectRequest.copy(user=uid).hydrate
        fullRequestFuture.flatMap({
          case None=>
            Future(BadRequest(Json.obj("status"->"error","detail"->"Invalid template or storage ID")))
          case Some(rq)=>
            projectHelper.create(rq,None).map({
              case Failure(error)=>
                logger.error("Could not create new project", error)
                InternalServerError(Json.obj("status"->"error","detail"->error.toString))
              case Success(projectEntry)=>
                logger.error(s"Created new project: $projectEntry")
                Ok(Json.obj("status"->"ok","detail"->"created project", "projectId"->projectEntry.id.get))
            })
        })
      })
  }}

}
