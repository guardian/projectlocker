package controllers

import javax.inject.Inject

import com.unboundid.ldap.sdk.LDAPConnectionPool
import models._
import play.api.Configuration
import play.api.cache.SyncCacheApi
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery
import slick.jdbc.PostgresProfile.api._

import scala.util.{Failure, Success}

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by localhome on 17/01/2017.
  */
class ProjectTypeController @Inject() (config: Configuration, dbConfigProvider: DatabaseConfigProvider,
                                       cacheImpl:SyncCacheApi)
  extends GenericDatabaseObjectController[ProjectType] with ProjectTypeSerializer{
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  implicit val db = dbConfig.db

  implicit val cache:SyncCacheApi = cacheImpl
  override def deleteid(requestedId: Int) = dbConfig.db.run(
    TableQuery[ProjectTypeRow].filter(_.id === requestedId).delete.asTry
  )
  override def selectid(requestedId: Int) = dbConfig.db.run(
    TableQuery[ProjectTypeRow].filter(_.id === requestedId).result.asTry
  )

  override def selectall(startAt:Int, limit:Int) = dbConfig.db.run(
    TableQuery[ProjectTypeRow].drop(startAt).take(limit).result.asTry //simple select *
  )

  override def jstranslate(result: Seq[ProjectType]) = result
  override def jstranslate(result: ProjectType) = result  //implicit translation should handle this

  override def insert(entry: ProjectType,uid:String) = dbConfig.db.run(
    (TableQuery[ProjectTypeRow] returning TableQuery[ProjectTypeRow].map(_.id) += entry).asTry
  )

  override def dbupdate(itemId:Int, entry:ProjectType) = {
    val newRecord = entry.id match {
      case Some(id)=>entry
      case None=>entry.copy(id=Some(itemId))
    }
    dbConfig.db.run(
      TableQuery[ProjectTypeRow].filter(_.id===itemId).update(newRecord).asTry
    )
  }

  override def validate(request:Request[JsValue]) = request.body.validate[ProjectType]

  def listPostrun(projectTypeId: Int) = IsAuthenticatedAsync {uid=>{request=>
    PostrunAssociation.entriesForProjectType(projectTypeId).map({
      case Success(result)=>Ok(Json.obj("status"->"ok","result"->result.map(_._2)))
      case Failure(error)=>
        logger.error(error.toString)
        InternalServerError(Json.obj("status"->"error","detail"->error.toString))
    })
  }}
}
