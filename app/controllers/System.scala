package controllers

import com.google.inject.Inject
import play.api.{Configuration, Logger}
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json._
import play.api.mvc.{Action, BodyParsers, Controller}
import slick.driver.JdbcProfile

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import helpers.DatabaseHelper

class System @Inject() (configuration: Configuration, dbConfigProvider: DatabaseConfigProvider,databaseHelper:DatabaseHelper) extends Controller{
  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  private val logger: Logger = Logger(this.getClass)

  def init = Action.async {
      databaseHelper.setUpDB().map({
      case Success(result)=>
        logger.info("Database succesfully initialised")
        Ok(Json.obj("status"->"ok","detail"->"database initialised"))
      case Failure(error)=>
        logger.error(error.toString)
        InternalServerError(Json.obj("status"->"error", "detail"->error.toString))
    })
  }
}
