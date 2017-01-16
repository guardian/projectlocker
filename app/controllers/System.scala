package controllers


import com.google.inject.Inject
import models._
import play.api.Configuration
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{Action, BodyParsers, Controller}
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.PostgresDriver.api._

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

class System @Inject() (configuration: Configuration, dbConfigProvider: DatabaseConfigProvider) extends Controller{
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  def init = Action.async {
    dbConfig.db.run(
      DBIO.seq(
        (
          TableQuery[FileAssociationRow].schema ++
          TableQuery[FileEntryRow].schema ++
          TableQuery[ProjectEntryRow].schema ++
          TableQuery[ProjectTemplateRow].schema ++
          TableQuery[ProjectTypeRow].schema ++
          TableQuery[StorageEntryRow].schema
          ).create
      ).asTry
    ).map({
      case Success(result)=>Ok(Json.obj("status"->"ok","detail"->"database initialised"))
      case Failure(error)=>InternalServerError(Json.obj("status"->"error", "detail"->error.toString))
    })
  }
}
