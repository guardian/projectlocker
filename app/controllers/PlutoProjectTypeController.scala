package controllers

import javax.inject._

import models._
import play.api.cache.SyncCacheApi
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.{JsResult, JsValue, Json}
import play.api.mvc.Request
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future
import scala.util.Try

@Singleton
class PlutoProjectTypeController @Inject()(dbConfigProvider:DatabaseConfigProvider, cacheImpl:SyncCacheApi)
  extends GenericDatabaseObjectController[PlutoProjectType] with PlutoProjectTypeSerializer {

  implicit val db=dbConfigProvider.get[PostgresProfile].db

  implicit val cache:SyncCacheApi = cacheImpl

  override def selectall(startAt: Int, limit: Int): Future[Try[Seq[PlutoProjectType]]] = db.run(
    TableQuery[PlutoProjectTypeRow].drop(startAt).take(limit).result.asTry
  )

  override def selectid(requestedId: Int): Future[Try[Seq[PlutoProjectType]]] = db.run(
    TableQuery[PlutoProjectTypeRow].filter(_.id===requestedId).result.asTry
  )

  override def insert(entry: PlutoProjectType, uid: String): Future[Try[Int]] = throw new RuntimeException("This is not supported")

  override def deleteid(requestedId: Int):Future[Try[Int]] = throw new RuntimeException("This is not supported")

  override def dbupdate(itemId: Int, entry:PlutoProjectType):Future[Try[Int]] = throw new RuntimeException("This is not supported")

  /*these are handled through implict translation*/
  override def jstranslate(result:Seq[PlutoProjectType]):Json.JsValueWrapper = result
  override def jstranslate(result:PlutoProjectType):Json.JsValueWrapper = result

  override def validate(request: Request[JsValue]): JsResult[PlutoProjectType] = request.body.validate[PlutoProjectType]
}