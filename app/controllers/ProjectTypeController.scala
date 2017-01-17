package controllers

import com.google.inject.Inject
import models.{ProjectType, ProjectTypeRow, ProjectTypeSerializer}
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
class ProjectTypeController @Inject() (config: Configuration, dbConfigProvider: DatabaseConfigProvider)
  extends GenericDatabaseObjectController[ProjectType] with ProjectTypeSerializer{
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  override def selectall = dbConfig.db.run(
    TableQuery[ProjectTypeRow].result.asTry //simple select *
  )

  override def jstranslate(result: Seq[ProjectType]) = result

  override def insert(entry: ProjectType) = dbConfig.db.run(
    (TableQuery[ProjectTypeRow] returning TableQuery[ProjectTypeRow].map(_.id) += entry).asTry
  )

  override def validate(request:Request[JsValue]) = request.body.validate[ProjectType]
}
