package controllers

import javax.inject.Inject
import models.{ProjectEntry, ProjectEntryRow, ProjectEntrySerializer, StorageEntryRow}
import play.api.Configuration
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.JsValue
import play.api.mvc.Request
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.PostgresDriver.api._

/**
  * Created by localhome on 17/01/2017.
  */
class ProjectEntryController @Inject() (config: Configuration, dbConfigProvider: DatabaseConfigProvider)
  extends GenericDatabaseObjectController[ProjectEntry] with ProjectEntrySerializer
{
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  override def deleteid(requestedId: Int) = dbConfig.db.run(
    TableQuery[ProjectEntryRow].filter(_.id === requestedId).delete.asTry
  )

  override def selectid(requestedId: Int) = dbConfig.db.run(
    TableQuery[ProjectEntryRow].filter(_.id === requestedId).result.asTry
  )

  override def selectall = dbConfig.db.run(
    TableQuery[ProjectEntryRow].result.asTry //simple select *
  )

  override def jstranslate(result: Seq[ProjectEntry]) = result
  override def jstranslate(result: ProjectEntry) = result  //implicit translation should handle this

  override def insert(entry: ProjectEntry) = dbConfig.db.run(
    (TableQuery[ProjectEntryRow] returning TableQuery[ProjectEntryRow].map(_.id) += entry).asTry
  )

  override def validate(request:Request[JsValue]) = request.body.validate[ProjectEntry]
}
