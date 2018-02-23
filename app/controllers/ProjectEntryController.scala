package controllers

import javax.inject.{Inject, Singleton}

import auth.Security
import com.unboundid.ldap.sdk.LDAPConnectionPool
import exceptions.{BadDataException, RecordNotFoundException}
import helpers.ProjectCreateHelper
import models._
import play.api.cache.SyncCacheApi
import play.api.{Configuration, Logger}
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.JsValue
import play.api.mvc._
import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery
import slick.jdbc.PostgresProfile.api._
import play.api.libs.json.{JsError, Json}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

/**
  * Created by localhome on 17/01/2017.
  */
@Singleton
class ProjectEntryController @Inject() (cc:ControllerComponents, config: Configuration,
                                        dbConfigProvider: DatabaseConfigProvider, projectHelper:ProjectCreateHelper,
                                        cacheImpl:SyncCacheApi)
  extends GenericDatabaseObjectController[ProjectEntry]
    with ProjectEntrySerializer with ProjectRequestSerializer with UpdateTitleRequestSerializer with Security
{
  override implicit val cache:SyncCacheApi = cacheImpl

  val dbConfig = dbConfigProvider.get[JdbcProfile]

  override def deleteid(requestedId: Int) = dbConfig.db.run(
    TableQuery[ProjectEntryRow].filter(_.id === requestedId).delete.asTry
  )

  override def selectid(requestedId: Int) = dbConfig.db.run(
    TableQuery[ProjectEntryRow].filter(_.id === requestedId).result.asTry
  )

  protected def selectVsid(vsid: String) = dbConfig.db.run(
    TableQuery[ProjectEntryRow].filter(_.vidispineProjectId === vsid).result.asTry
  )

  def getByVsid(vsid:String) = IsAuthenticatedAsync {uid=>{request=>
    selectVsid(vsid).map({
      case Success(result)=>
        if(result.isEmpty)
          NotFound("")
        else
          Ok(Json.obj("status"->"ok","result"->this.jstranslate(result.head)))
      case Failure(error)=>
        logger.error(error.toString)
        InternalServerError(Json.obj("status"->"error","detail"->error.toString))
    })
  }}

  def doUpdateTitle(requestedId:Int, newTitle:String) = selectid(requestedId).flatMap({
      case Success(someSeq)=>
        someSeq.headOption match {
          case Some(record)=>
            val updatedProjectEntry = record.copy (projectTitle = newTitle)
            dbConfig.db.run (
            TableQuery[ProjectEntryRow].filter (_.id === requestedId).update (updatedProjectEntry).asTry
            )
          case None=>
            Future(Failure(new RecordNotFoundException(s"No record found for id $requestedId")))
        }
      case Failure(error)=>Future(Failure(error))
    })

  def doUpdateVsid(requestedId:Int, maybeNewVsid:Option[String]) = selectid(requestedId).flatMap({
    case Success(someSeq)=>
      someSeq.headOption match {
        case Some(record)=>
          val updatedProjectEntry = record.copy (vidispineProjectId = maybeNewVsid)
          dbConfig.db.run (
            TableQuery[ProjectEntryRow].filter (_.id === requestedId).update (updatedProjectEntry).asTry
          )
        case None=>
          Future(Failure(new RecordNotFoundException(s"No record found for id $requestedId")))
      }
    case Failure(error)=>Future(Failure(error))
  })

  def doUpdateTitle(vsid:String, newTitle:String) = selectVsid(vsid).flatMap({
    case Success(someSeq)=>
      someSeq.headOption match {
        case Some(record) =>
          val updatedProjectEntry = record.copy(projectTitle = newTitle)
          dbConfig.db.run(
            TableQuery[ProjectEntryRow].filter(_.vidispineProjectId === vsid).update(updatedProjectEntry).asTry
          )
        case None=>
          Future(Failure(new RecordNotFoundException(s"No record found for vsid $vsid")))
      }
    case Failure(error)=>Future(Failure(error))
  })

  def updateTitle(requestedId:Int) = IsAuthenticatedAsync(BodyParsers.parse.json) {uid=>{request=>
    request.body.validate[UpdateTitleRequest].fold(
      errors=>
        Future(BadRequest(Json.obj("status"->"error", "detail"->JsError.toJson(errors)))),
      updateTitleRequest=>
        doUpdateTitle(requestedId,updateTitleRequest.newTitle).map({
          case Success(rows)=>
            Ok(Json.obj("status"->"ok","detail"->"record updated"))
          case Failure(error)=>
            logger.error("Could not update project title", error)
            if(error.getClass==classOf[RecordNotFoundException])
              NotFound(Json.obj("status"->"error", "detail"-> s"record $requestedId not found"))
            else
              InternalServerError(Json.obj("status"->"error","detail"->error.toString))
        })
    )
  }}

  def updateTitleByVsid(vsid:String) = IsAuthenticatedAsync(BodyParsers.parse.json) {uid=>{request=>
    request.body.validate[UpdateTitleRequest].fold(
      errors=>
        Future(BadRequest(Json.obj("status"->"error", "detail"->JsError.toJson(errors)))),
      updateTitleRequest=>
        doUpdateTitle(vsid,updateTitleRequest.newTitle).map({
          case Success(rows)=>
            Ok(Json.obj("status"->"ok","detail"->"record updated"))
          case Failure(error)=>
            logger.error("Could not update project title", error)
            if(error.getClass==classOf[RecordNotFoundException])
              NotFound(Json.obj("status"->"error", "detail"-> s"record for $vsid not found"))
            else
              InternalServerError(Json.obj("status"->"error","detail"->error.toString))
        })
    )
  }}

  def updateVsid(requestedId:Int) = IsAuthenticatedAsync(BodyParsers.parse.json) {uid=>{request=>
    request.body.validate[UpdateTitleRequest].fold(
      errors=>
        Future(BadRequest(Json.obj("status"->"error", "detail"->JsError.toJson(errors)))),
      updateTitleRequest=>
        doUpdateVsid(requestedId,updateTitleRequest.newVsid).map({
          case Success(rows)=>
            Ok(Json.obj("status"->"ok","detail"->"record updated"))
          case Failure(error)=>
            logger.error("Could not update project title", error)
            if(error.getClass==classOf[RecordNotFoundException])
              NotFound(Json.obj("status"->"error", "detail"-> s"record $requestedId not found"))
            else
              InternalServerError(Json.obj("status"->"error","detail"->error.toString))
        })
    )
  }}

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
