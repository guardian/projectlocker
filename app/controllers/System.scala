package controllers

import javax.inject.Inject

import auth.Security
import com.unboundid.ldap.sdk.LDAPConnectionPool
import play.api.{Configuration, Logger}
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json._
import play.api.mvc._
import slick.jdbc.PostgresProfile

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import helpers.DatabaseHelper
import play.api.cache.SyncCacheApi

class System @Inject() (cc:ControllerComponents, configuration: Configuration, dbConfigProvider: DatabaseConfigProvider,
                        databaseHelper:DatabaseHelper,cacheImpl:SyncCacheApi)
  extends AbstractController(cc) with Security {

  implicit val cache:SyncCacheApi = cacheImpl
  private val dbConfig = dbConfigProvider.get[PostgresProfile]

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
