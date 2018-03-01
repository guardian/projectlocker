package controllers

import javax.inject.{Inject, Singleton}

import models._
import play.api.Configuration
import play.api.cache.SyncCacheApi
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery
import slick.jdbc.PostgresProfile.api._
import play.api.libs.json._

import scala.concurrent.Future
import scala.util.Try

@Singleton
class PostrunActionController  @Inject() (config: Configuration, dbConfigProvider: DatabaseConfigProvider,
                                          cacheImpl:SyncCacheApi)
  extends GenericDatabaseObjectController[PostrunAction] with PostrunActionSerializer {

  implicit val cache:SyncCacheApi = cacheImpl
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  override def selectid(requestedId: Int): Future[Try[Seq[PostrunAction]]] = dbConfig.db.run(
    TableQuery[PostrunActionRow].filter(_.id === requestedId).result.asTry
  )

  override def selectall(startAt: Int, limit: Int): Future[Try[Seq[PostrunAction]]] = dbConfig.db.run(
    TableQuery[PostrunActionRow].drop(startAt).take(limit).result.asTry
  )

  override def jstranslate(result: PostrunAction): Json.JsValueWrapper = result

  override def jstranslate(result: Seq[PostrunAction]): Json.JsValueWrapper = result  //PostrunActionSerializer is implicitly called to do this

  override def deleteid(requestedId: Int) = dbConfig.db.run(
    TableQuery[PostrunActionRow].filter(_.id === requestedId).delete.asTry
  )

  override def validate(request: Request[JsValue]) = request.body.validate[PostrunAction]

  override def insert(entry: PostrunAction, uid:String) = dbConfig.db.run(
    (TableQuery[PostrunActionRow] returning TableQuery[PostrunActionRow].map(_.id) += entry).asTry)

}
