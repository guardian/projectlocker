package controllers

import com.google.inject.Inject
import models.{ProjectEntryRow, StorageEntry, StorageEntryRow, StorageSerializer}
import play.api.Configuration
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json._
import play.api.mvc.{Action, BodyParsers, Request}
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.PostgresDriver.api._

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

class StoragesController @Inject()
    (configuration: Configuration, dbConfigProvider: DatabaseConfigProvider)
    extends GenericDatabaseObjectController[StorageEntry] with StorageSerializer {

  implicit val dbConfig:DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]

  override def selectid(requestedId: Int) = dbConfig.db.run(
    TableQuery[StorageEntryRow].filter(_.id === requestedId).result.asTry
  )

  override def deleteid(requestedId: Int) = dbConfig.db.run(
    TableQuery[StorageEntryRow].filter(_.id === requestedId).delete.asTry
  )

  override def selectall = dbConfig.db.run(
    TableQuery[StorageEntryRow].result.asTry //simple select *
  )

  override def jstranslate(result: Seq[StorageEntry]) = result.asInstanceOf[Seq[StorageEntry]]  //implicit translation should handle this
  override def jstranslate(result: StorageEntry) = result  //implicit translation should handle this

  override def insert(storageEntry: StorageEntry) = dbConfig.db.run(
    (TableQuery[StorageEntryRow] returning TableQuery[StorageEntryRow].map(_.id) += storageEntry).asTry
  )

  override def validate(request:Request[JsValue]) = request.body.validate[StorageEntry]
}
