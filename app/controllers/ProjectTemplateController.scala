package controllers

import com.google.inject.Inject
import models._
import play.api.Configuration
import play.api.mvc._
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json._
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.PostgresDriver.api._

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

class ProjectTemplateController @Inject() (config: Configuration, dbConfigProvider: DatabaseConfigProvider)
  extends GenericDatabaseObjectController[ProjectTemplate] with ProjectTemplateSerializer with StorageSerializer{

  val dbConfig = dbConfigProvider.get[JdbcProfile]

  override def selectid(requestedId: Int) = dbConfig.db.run(
    TableQuery[ProjectTemplateRow].filter(_.id === requestedId).result.asTry
  )

  override def validate(request: Request[JsValue]) = request.body.validate[ProjectTemplate]

  override def selectall = dbConfig.db.run(TableQuery[ProjectTemplateRow].result.asTry)

  override def insert(entry: ProjectTemplate) = dbConfig.db.run(
    (TableQuery[ProjectTemplateRow] returning TableQuery[ProjectTemplateRow].map(_.id) += entry).asTry)

  override def jstranslate(result: Seq[ProjectTemplate]) = result
  override def jstranslate(result: ProjectTemplate) = result  //implicit translation should handle this
}
