package controllers

import akka.actor.ActorSystem
import akka.stream.Materializer
import auth.BearerTokenAuth
import javax.inject.{Inject, Singleton}
import models.{ProjectEntryRow, StorageEntry, StorageEntryRow, StorageSerializer, StorageType, StorageTypeSerializer}
import play.api.Configuration
import play.api.cache.SyncCacheApi
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json._
import play.api.mvc.{Action, BodyParsers, ControllerComponents, Request}
import slick.basic.DatabaseConfig
import slick.jdbc.PostgresProfile
import slick.lifted.TableQuery
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class StoragesController @Inject()
    (override val controllerComponents:ControllerComponents, override val bearerTokenAuth:BearerTokenAuth,
     configuration: Configuration, dbConfigProvider: DatabaseConfigProvider, cacheImpl:SyncCacheApi)
    (implicit mat:Materializer, system:ActorSystem)
    extends GenericDatabaseObjectController[StorageEntry] with StorageSerializer with StorageTypeSerializer {

  implicit val cache:SyncCacheApi = cacheImpl

  val knownTypes = List(
    StorageType("Local",needsLogin=false,hasSubfolders=true, canVersion = false),
    StorageType("ObjectMatrix",needsLogin = true,hasSubfolders = false, canVersion=true),
    StorageType("S3",needsLogin = true,hasSubfolders = true,canVersion=true)
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
  ).map(_.map(_.map(_.copy(password=Some("****")))))

  override def jstranslate(result: Seq[StorageEntry]) = result.asInstanceOf[Seq[StorageEntry]]  //implicit translation should handle this
  override def jstranslate(result: StorageEntry) = result  //implicit translation should handle this

  override def insert(storageEntry: StorageEntry,uid:String) = dbConfig.db.run(
    (TableQuery[StorageEntryRow] returning TableQuery[StorageEntryRow].map(_.id) += storageEntry).asTry
  )

  /**
    * sets the password field in the provided [[StorageEntry]] model to the same value as the current one
    * this is so that updates can be made to the record without sending storage passwords to the client and back again every time
    * @param itemId item ID to update
    * @param updatedEntry updated storage entry
    * @return a Future, contianing a Try, containing the updated storage entry with the previous password set
    */
  def reconstitutePassword(itemId:Int, updatedEntry:StorageEntry) = {
    val maybeRealPasswordFut = selectid(itemId).map(_.map(_.headOption.flatMap(_.password)))

    maybeRealPasswordFut.map(_.map(maybeRealPassword => updatedEntry.copy(password = maybeRealPassword)))
  }

  /**
    * protocol method to perform update on an entry. This implementation has a special behaviour where if the password field is set to
    * '****' (four asterisks) it means "don't change password" and the previous password is retrieved and placed into the update
    * @param itemId item ID to update
    * @param entry new record to overwrite it with
    */
  override def dbupdate(itemId:Int, entry:StorageEntry) = {
    val reconsEntryFuture = if(entry.password.contains("****")){  //if the password is set to this special value, we don't want to change it.
      //grab the existing value and use that.
      reconstitutePassword(itemId, entry).map({ //this is a bit messy but will do for the time being. We want a Future that
        //fails on error, so we can compose later on, but the protocol requires a Future[Try] from selectid. So squash it here.
        case Success(result)=>result
        case Failure(err)=>throw err
      })
    } else {
      Future(entry)
    }

    reconsEntryFuture.flatMap(reconsEntry=> {
      val newRecord = reconsEntry.id match {
        case Some(id) => reconsEntry
        case None => reconsEntry.copy(id = Some(itemId))
      }
      dbConfig.db.run(
        TableQuery[StorageEntryRow].filter(_.id === itemId).update(newRecord).asTry
      )
    })
  }

  override def validate(request:Request[JsValue]) = request.body.validate[StorageEntry]

  def types = IsAuthenticated {uid=> {request=>
    Ok(Json.obj("status"->"ok","types"->this.knownTypes))
  }}

  override def shouldCreateEntry(newEntry: StorageEntry): Either[String, Boolean] = {
    logger.debug(s"Got new entry $newEntry")

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
