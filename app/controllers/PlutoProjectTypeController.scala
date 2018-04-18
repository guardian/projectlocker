package controllers

import javax.inject._

import models._
import play.api.cache.SyncCacheApi
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.{JsResult, JsValue, Json}
import play.api.mvc.Request
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class PlutoProjectTypeController @Inject()(dbConfigProvider:DatabaseConfigProvider, cacheImpl:SyncCacheApi)
  extends GenericDatabaseObjectController[PlutoProjectType] with PlutoProjectTypeSerializer {

  implicit val db=dbConfigProvider.get[PostgresProfile].db

  implicit val cache:SyncCacheApi = cacheImpl

  override def selectall(startAt: Int, limit: Int): Future[Try[Seq[PlutoProjectType]]] = db.run(
    TableQuery[PlutoProjectTypeRow].drop(startAt).take(limit).result.asTry
  )

  override def selectid(requestedId: Int): Future[Try[Seq[PlutoProjectType]]] = db.run(
    TableQuery[PlutoProjectTypeRow].filter(_.id===requestedId).result.asTry
  )

  override def insert(entry: PlutoProjectType, uid: String): Future[Try[Int]] = throw new RuntimeException("This is not supported")

  override def deleteid(requestedId: Int):Future[Try[Int]] = throw new RuntimeException("This is not supported")

  override def dbupdate(itemId: Int, entry:PlutoProjectType):Future[Try[Int]] = throw new RuntimeException("This is not supported")

  /*these are handled through implict translation*/
  override def jstranslate(result:Seq[PlutoProjectType]):Json.JsValueWrapper = result
  override def jstranslate(result:PlutoProjectType):Json.JsValueWrapper = result

  override def validate(request: Request[JsValue]): JsResult[PlutoProjectType] = request.body.validate[PlutoProjectType]

  def setDefaultProjectTemplate(requestedId: Int, projectTemplateId: Int) = IsAdminAsync {uid=>{ request=>
    selectid(requestedId).flatMap({
      case Success(projectTypes)=>
        projectTypes.headOption match { //matching on a unique key so there will only be zero or one
          case Some(projectType)=>
            val updatedProjectType = projectType.copy(defaultProjectTemplate=Some(projectTemplateId))
            db.run(
              TableQuery[PlutoProjectTypeRow].filter(_.id===requestedId).update(updatedProjectType).asTry
            ).map({
              case Success(updatedRows)=>
                Ok(Json.obj("status"->"ok","detail"->s"updated $updatedRows records"))
              case Failure(error)=>
                logger.error("Could not update pluto project type record: ", error)
                InternalServerError(Json.obj("status"->"error","detail"->error.toString))
            })
          case None=>
            Future(NotFound(Json.obj("status"->"notfound","detail"->s"pluto project type $requestedId not found")))
        }
      case Failure(error)=>
        logger.error("Could not look up pluto project type record: ", error)
        Future(InternalServerError(Json.obj("status"->"error","detail"->error.toString)))
    })
  }}

  def removeDefaultProjectTemplate(requestedId: Int) = IsAdminAsync {uid=> {request=>
    selectid(requestedId).flatMap({
      case Success(projectTypes)=>
        projectTypes.headOption match {
          case Some(projectType) =>
            val updatedProjectType = projectType.copy(defaultProjectTemplate = None)
            db.run(
              TableQuery[PlutoProjectTypeRow].filter(_.id === requestedId).update(updatedProjectType).asTry
            ).map({
              case Success(updatedRows) =>
                Ok(Json.obj("status" -> "ok", "detail" -> s"updated $updatedRows records"))
              case Failure(error) =>
                logger.error("Could not update pluto project type record: ", error)
                InternalServerError(Json.obj("status" -> "error", "detail" -> error.toString))
            })
          case None =>
            Future(NotFound(Json.obj("status" -> "notfound", "detail" -> s"pluto project type $requestedId not found")))
        }
      case Failure(error)=>
        logger.error("Could not look up pluto project type record: ", error)
        Future(InternalServerError(Json.obj("status"->"error","detail"->error.toString)))
    })
  }}
}