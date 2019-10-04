package controllers

import javax.inject.{Inject, Singleton}
import models.{ProjectEntryRow, StorageEntry, StorageEntryRow, StorageSerializer, StorageType, StorageTypeSerializer}
import play.api.Configuration
import play.api.cache.SyncCacheApi
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json._
import play.api.mvc.{Action, BodyParsers, Request}
import slick.basic.DatabaseConfig
import slick.jdbc.PostgresProfile
import slick.lifted.TableQuery
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class StoragesController @Inject()
    (configuration: Configuration, dbConfigProvider: DatabaseConfigProvider, cacheImpl:SyncCacheApi)
    extends GenericDatabaseObjectController[StorageEntry] with StorageSerializer with StorageTypeSerializer {

  implicit val cache:SyncCacheApi = cacheImpl

  val knownTypes = List(
    StorageType("Local",needsLogin=false,hasSubfolders=true),
    StorageType("ObjectMatrix",needsLogin = true,hasSubfolders = false),
    StorageType("S3",needsLogin = true,hasSubfolders = true)
  )

  implicit val dbConfig:DatabaseConfig[PostgresProfile] = dbConfigProvider.get[PostgresProfile]

  override def selectid(requestedId: Int) = dbConfig.db.run(
    TableQuery[StorageEntryRow].filter(_.id === requestedId).result.asTry
  )

  override def deleteid(requestedId: Int) = dbConfig.db.run(
    TableQuery[StorageEntryRow].filter(_.id === requestedId).delete.asTry
  )

  override def selectall(startAt:Int, limit:Int) = dbConfig.db.run(
    TableQuery[StorageEntryRow].drop(startAt).take(limit).result.asTry //simple select *
  )

  override def jstranslate(result: Seq[StorageEntry]) = result.asInstanceOf[Seq[StorageEntry]]  //implicit translation should handle this
  override def jstranslate(result: StorageEntry) = result  //implicit translation should handle this

  override def insert(storageEntry: StorageEntry,uid:String) = dbConfig.db.run(
    (TableQuery[StorageEntryRow] returning TableQuery[StorageEntryRow].map(_.id) += storageEntry).asTry
  )

  override def dbupdate(itemId:Int, entry:StorageEntry) = {
    val newRecord = entry.id match {
      case Some(id)=>entry
      case None=>entry.copy(id=Some(itemId))
    }
    dbConfig.db.run(
      TableQuery[StorageEntryRow].filter(_.id===itemId).update(newRecord).asTry
    )
  }

  override def validate(request:Request[JsValue]) = request.body.validate[StorageEntry]

  def types = IsAuthenticated {uid=> {request=>
    Ok(Json.obj("status"->"ok","types"->this.knownTypes))
  }}

  override def shouldCreateEntry(newEntry: StorageEntry): Either[String, Boolean] = {
    newEntry.rootpath match {
      case Some(rootpath) =>
        newEntry.getStorageDriver match {
          case Some(storageDriver) =>
            if(storageDriver.pathExists(rootpath, 0))
              Right(true)
            else
              Left(s"Path $rootpath does not exist")
          case None=>
            Left(s"No storage driver defined for storage type ${newEntry.storageType}")
        }
      case None=>
        Left("No root path was set for the storage")
    }
  }
}
