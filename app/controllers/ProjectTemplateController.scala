package controllers

import javax.inject.Inject
import com.unboundid.ldap.sdk.LDAPConnectionPool
import models._
import play.api.cache.SyncCacheApi
import play.api.{Configuration, Logger}
import play.api.mvc._
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json._
import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

class ProjectTemplateController @Inject() (config: Configuration, dbConfigProvider: DatabaseConfigProvider,
                                           cacheImpl:SyncCacheApi)
  extends GenericDatabaseObjectController[ProjectTemplate] with ProjectTemplateSerializer with StorageSerializer{

  implicit val cache:SyncCacheApi = cacheImpl
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  override def deleteid(requestedId: Int) = dbConfig.db.run(
    TableQuery[ProjectTemplateRow].filter(_.id === requestedId).delete.asTry
  )

  override def selectid(requestedId: Int) = dbConfig.db.run(
    TableQuery[ProjectTemplateRow].filter(_.id === requestedId).result.asTry
  )

  override def validate(request: Request[JsValue]) = request.body.validate[ProjectTemplate]

  override def selectall(startAt:Int, limit:Int) = dbConfig.db.run(TableQuery[ProjectTemplateRow].drop(startAt).take(limit).result.asTry)

  override def insert(entry: ProjectTemplate, uid:String) = dbConfig.db.run(
    (TableQuery[ProjectTemplateRow] returning TableQuery[ProjectTemplateRow].map(_.id) += entry).asTry)

  override def jstranslate(result: Seq[ProjectTemplate]) = result
  override def jstranslate(result: ProjectTemplate) = result  //implicit translation should handle this

  override def dbupdate(itemId:Int, entry:ProjectTemplate) = {
    val newRecord = entry.id match {
      case Some(id)=>entry
      case None=>entry.copy(id=Some(itemId))
    }
    dbConfig.db.run(
      TableQuery[ProjectTemplateRow].filter(_.id===itemId).update(newRecord).asTry
    )
  }

  /* custom implementation of deleteAction to reflect whether the previous file delete operation succeeded or not */
  def deleteAction(requestedId: Int, didDeleteFile: Boolean): Future[Result] = {
    deleteid(requestedId).map({
      case Success(result)=>
        if(result==0)
          NotFound(Json.obj("status" -> "notfound", "id"->requestedId))
        else {
          if (didDeleteFile)
            Ok(Json.obj("status" -> "ok", "detail" -> "deleted", "id" -> requestedId))
          else
            Ok(Json.obj("status" -> "warning", "detail" -> "Template deleted but file could not be deleted", "id" -> requestedId))
        }
      case Failure(error)=>handleConflictErrors(error,"file",isInsert = false)
    })
  }

  override def delete(requestedId: Int) = Action.async { request =>
    implicit val db:slick.jdbc.JdbcProfile#Backend#Database=dbConfig.db

    if(requestedId<0)
      Future(Conflict(Json.obj("status"->"error","detail"->"This object is still referred to by sub-objects")))
    else {
      /* step one - get the file for the template object that we want to delete */
      val fileFuture = selectid(requestedId).flatMap({
        case Success(templatesList) =>
          val template = templatesList.head
          template.file
        case Failure(error) =>
          logger.error(error.toString)
          throw error; //this will result in the future failing and can be picked up as a Try later on
      })

      /* step two - actually try to delete the file from disk */
      val fileDeleteFuture = fileFuture.flatMap(_.deleteFromDisk)

      /* step three - delete the file object representing it */
      val fileObjectDeleteFuture = fileDeleteFuture.map({
        case Left(errormsg) => Left(errormsg)
        case Right(didDelete) =>
          val fileEntry = fileFuture.value.get.get //we know that fileFuture completed successfully or fileDeleteFuture won't succeed
          if (didDelete) fileEntry.deleteSelf
      })

      /*step four - now delete the template object that was using it */
      val templateDeleteFuture = fileDeleteFuture.flatMap({
        case Left(errorString) =>
          logger.error(s"Not able to delete the underlying file: $errorString")
          Future(InternalServerError(Json.obj("status" -> "error", "detail" -> s"Not able to delete the underlying file: $errorString")))
        case Right(managedDelete) =>
            /*now run the normal delete process for the project template object, even if the file could not be deleted (maybe it doesn't exist
          any more). Use a custom implentation of deleteAction to warn the frontend in this case*/
            deleteAction(requestedId, managedDelete)
      })

      templateDeleteFuture
    }
  }
}
