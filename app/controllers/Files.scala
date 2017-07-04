package controllers

import com.google.inject.Inject
import play.api.Configuration
import play.api.db.slick.DatabaseConfigProvider
import play.api.mvc.{Action, BodyParsers, Controller, Request}
import slick.driver.JdbcProfile
import play.api.libs.json._
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global
import models._
import slick.lifted.TableQuery


class Files @Inject() (configuration: Configuration, dbConfigProvider: DatabaseConfigProvider)
  extends GenericDatabaseObjectController[FileEntry] with FileEntrySerializer {

  val dbConfig = dbConfigProvider.get[JdbcProfile]

  override def selectid(requestedId: Int) = dbConfig.db.run(
    TableQuery[FileEntryRow].filter(_.id === requestedId).result.asTry
  )

  override def selectall = dbConfig.db.run(
    TableQuery[FileEntryRow].result.asTry //simple select *
  )

  override def jstranslate(result: Seq[FileEntry]) = result  //implicit translation should handle this
  override def jstranslate(result: FileEntry) = result  //implicit translation should handle this

  override def insert(entry: FileEntry) = dbConfig.db.run(
    (TableQuery[FileEntryRow] returning TableQuery[FileEntryRow].map(_.id) += entry).asTry
  )

  override def validate(request:Request[JsValue]) = request.body.validate[FileEntry]
}
