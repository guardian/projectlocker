package controllers

import javax.inject.Inject

import play.api.libs.json._
import auth.Security
import play.api.{Configuration, Logger}
import play.api.db.slick.DatabaseConfigProvider
import play.api.mvc._
import slick.jdbc.PostgresProfile

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.cache.SyncCacheApi

class System @Inject() (cc:ControllerComponents, configuration: Configuration, dbConfigProvider: DatabaseConfigProvider,
                        cacheImpl:SyncCacheApi)
  extends AbstractController(cc) with Security {

  implicit val cache:SyncCacheApi = cacheImpl
  implicit val config:Configuration = configuration
  private val dbConfig = dbConfigProvider.get[PostgresProfile]

  def plutoconfig = IsAuthenticated {uid=>{request=>
    Ok(Json.obj(
      "status"->"ok",
      "plutoServer"->configuration.getOptional[String]("pluto.server_url"),
      "syncEnabled"->configuration.getOptional[String]("pluto.sync_enabled"),
      "siteName"->configuration.getOptional[String]("pluto.site_name")
    ))
  }}
}
