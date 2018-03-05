package controllers

import javax.inject.{Inject, Singleton}

import exceptions.AlreadyExistsException
import models._
import play.api.Configuration
import play.api.cache.SyncCacheApi
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery
import slick.jdbc.PostgresProfile.api._
import play.api.libs.json._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class PostrunActionController  @Inject() (config: Configuration, dbConfigProvider: DatabaseConfigProvider,
                                          cacheImpl:SyncCacheApi)
  extends GenericDatabaseObjectController[PostrunAction] with PostrunActionSerializer {

  implicit val cache:SyncCacheApi = cacheImpl
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  override def selectid(requestedId: Int): Future[Try[Seq[PostrunAction]]] = dbConfig.db.run(
    TableQuery[PostrunActionRow].filter(_.id === requestedId).result.asTry
  )

  override def selectall(startAt: Int, limit: Int): Future[Try[Seq[PostrunAction]]] = dbConfig.db.run(
    TableQuery[PostrunActionRow].drop(startAt).take(limit).result.asTry
  )

  override def jstranslate(result: PostrunAction): Json.JsValueWrapper = result

  override def jstranslate(result: Seq[PostrunAction]): Json.JsValueWrapper = result  //PostrunActionSerializer is implicitly called to do this

  override def deleteid(requestedId: Int) = dbConfig.db.run(
    TableQuery[PostrunActionRow].filter(_.id === requestedId).delete.asTry
  )

  override def validate(request: Request[JsValue]) = request.body.validate[PostrunAction]

  override def insert(entry: PostrunAction, uid:String) = dbConfig.db.run(
    (TableQuery[PostrunActionRow] returning TableQuery[PostrunActionRow].map(_.id) += entry).asTry)

  def insertAssociation(postrunId: Int, projectTypeId: Int) = dbConfig.db.run(
    (TableQuery[PostrunAssociationRow] returning TableQuery[PostrunAssociationRow].map(_.id) += (projectTypeId, postrunId)).asTry
  )

  def removeAssociation(postrunId: Int, projectTypeId: Int) =dbConfig.db.run(
    TableQuery[PostrunAssociationRow].filter(_.postrunEntry===postrunId).filter(_.projectType===projectTypeId).delete.asTry
  )

  def associate(postrunId: Int, projectTypeId: Int) = Action.async {
    insertAssociation(postrunId, projectTypeId).map({
      case Success(newRowId)=>Ok(Json.obj("status"->"ok", "detail"->s"added association with id newRowId"))
      case Failure(error)=>
        logger.error("Could not create postrun association:", error)
        val errorString = error.toString
        if(errorString.contains("violates foreign key constraint") || errorString.contains("Referential integrity constraint violation"))
          Conflict(Json.obj("status"->"error","detail"->"This association either already exists or refers to objects which do not exist"))
        else
          InternalServerError(Json.obj("status"->"error","detail"->error.toString))
    })
  }

  def unassociate(postrunId: Int, projectTypeId: Int) = Action.async {
    removeAssociation(postrunId, projectTypeId) map {
      case Success(affectedRows)=>Ok(Json.obj("status"->"ok", "detail"->"removed association"))
      case Failure(error)=>
        logger.error("Could not remove postrun association:", error)
        val errorString = error.toString
        if(errorString.contains("violates foreign key constraint") || errorString.contains("Referential integrity constraint violation"))
          Conflict(Json.obj("status"->"error","detail"->"This association is still referred to by other objects"))
        else
          InternalServerError(Json.obj("status"->"error","detail"->error.toString))
    }
  }
}
