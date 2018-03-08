package controllers

import javax.inject.{Inject, Singleton}

import auth.Security
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
import scala.io.Source

@Singleton
class PostrunActionController  @Inject() (config: Configuration, dbConfigProvider: DatabaseConfigProvider,
                                          cacheImpl:SyncCacheApi)
  extends GenericDatabaseObjectController[PostrunAction] with PostrunActionSerializer with PostrunDependencySerializer with Security {

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

  override def dbupdate(itemId:Int, entry:PostrunAction) = {
    val newRecord = entry.id match {
      case Some(id)=>entry
      case None=>entry.copy(id=Some(itemId))
    }
    dbConfig.db.run(
      TableQuery[PostrunActionRow].filter(_.id===itemId).update(newRecord).asTry
    )
  }

  def insertAssociation(postrunId: Int, projectTypeId: Int) = dbConfig.db.run(
    (TableQuery[PostrunAssociationRow] returning TableQuery[PostrunAssociationRow].map(_.id) += (projectTypeId, postrunId)).asTry
  )

  def removeAssociation(postrunId: Int, projectTypeId: Int) =dbConfig.db.run(
    TableQuery[PostrunAssociationRow].filter(_.postrunEntry===postrunId).filter(_.projectType===projectTypeId).delete.asTry
  )

  def associate(postrunId: Int, projectTypeId: Int) = IsAuthenticatedAsync {uid=> { request =>
    insertAssociation(postrunId, projectTypeId).map({
      case Success(newRowId) => Ok(Json.obj("status" -> "ok", "detail" -> s"added association with id $newRowId"))
      case Failure(error) => handleConflictErrors(error, "postrun association", isInsert=true)
    })
  }}

  def unassociate(postrunId: Int, projectTypeId: Int) = IsAuthenticatedAsync {uid=>{request=>
    removeAssociation(postrunId, projectTypeId) map {
      case Success(affectedRows)=>Ok(Json.obj("status"->"ok", "detail"->"removed association"))
      case Failure(error)=>handleConflictErrors(error, "postrun association", isInsert=true)
    }
  }}

  def getSource(itemId:Int) = IsAuthenticatedAsync {uid=>{request=>
    implicit val configImplicit=config
    selectid(itemId) map {
      case Failure(error)=>
        logger.error("Could not load postrun source",error)
        InternalServerError(Json.obj("status"->"error","detail"->error.toString))
      case Success(rows)=>
        val scriptpath = rows.head.getScriptPath.toAbsolutePath
        try {
          Ok(Source.fromFile(scriptpath.toString).mkString).withHeaders("Content-Type" -> "text/x-python")
        } catch {
          case e:Throwable=>
            logger.error("Could not read postrun source code", e)
            InternalServerError(Json.obj("status"->"error","detail"->e.toString))
        }
    }
  }}

  def insertDependency(entry: PostrunDependency) = dbConfig.db.run(
    (TableQuery[PostrunDependencyRow] returning TableQuery[PostrunDependencyRow].map(_.id) += entry).asTry
  )

  def deleteDependency(entry: PostrunDependency) = dbConfig.db.run(
    TableQuery[PostrunDependencyRow].filter(_.sourceAction === entry.sourceAction).filter(_.dependsOn === entry.dependsOn).delete.asTry
  )

  def selectDependencies(postrunId:Int) = dbConfig.db.run(
    TableQuery[PostrunDependencyRow].filter(_.sourceAction===postrunId).result.asTry
  )

  def listDependencies(postrunId: Int) = IsAuthenticatedAsync {uid=>{request=>
    selectDependencies(postrunId).map({
      case Success(dependencyList)=>Ok(Json.obj("status"->"ok", "result"->dependencyList))
      case Failure(error)=>
        logger.error(s"Could not list postrun dependencies for $postrunId: ", error)
        InternalServerError(Json.obj("status"->"error", "detail"->error.toString))
    })
  }}

  def addDependency(sourceId:Int, dependsOn:Int) = IsAuthenticatedAsync {uid=>{request=>
    insertDependency(PostrunDependency(None,sourceId,dependsOn)).map({
      case Success(newRowId) => Ok(Json.obj("status" -> "ok", "detail" -> s"added dependency with id $newRowId"))
      case Failure(error) => handleConflictErrors(error,"postrun dependency", isInsert=true)
    })
  }}

  def removeDependency(sourceId:Int, dependsOn:Int) = IsAuthenticatedAsync {uid=>{request=>
    deleteDependency(PostrunDependency(None,sourceId,dependsOn)).map({
      case Success(newRowId) => Ok(Json.obj("status" -> "ok", "detail" -> s"deleted dependency"))
      case Failure(error) => handleConflictErrors(error,"postrun dependency", isInsert=false)
    })
  }}
}
