package controllers

import com.google.inject.Inject
import models.{StorageEntry, StorageEntryRow, StorageSerializer}
import play.api.Configuration
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.PostgresDriver.api.Tag
import play.api.libs.json._
import play.api.mvc.{Action, BodyParsers}
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.PostgresDriver.api._

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

class StoragesController @Inject()
    (override val configuration: Configuration, override val dbConfigProvider: DatabaseConfigProvider)
    extends GenericDatabaseObjectController[StorageEntry,StorageEntryRow] with StorageSerializer{

    override val objects = TableQuery[StorageEntryRow]
}
