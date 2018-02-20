package controllers

import javax.inject.Inject

import auth.{LDAPConnectionPoolWrapper, Security}
import com.unboundid.ldap.sdk.LDAPConnectionPool
import play.api.{Configuration, Logger}
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json._
import play.api.mvc._
import slick.driver.JdbcProfile

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import helpers.DatabaseHelper
import play.api.cache.SyncCacheApi

class System @Inject() (cc:ControllerComponents, configuration: Configuration, dbConfigProvider: DatabaseConfigProvider,
                        databaseHelper:DatabaseHelper,cacheImpl:SyncCacheApi, ldapPool:LDAPConnectionPoolWrapper)
  extends AbstractController(cc) with Security {

  implicit val cache:SyncCacheApi = cacheImpl
  implicit val ldapConnectionPool:LDAPConnectionPool = ldapPool.connectionPool.getOrElse(null)
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
