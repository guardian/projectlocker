package controllers
import javax.inject._

import models.{PlutoWorkingGroup, PlutoWorkingGroupRow, PlutoWorkingGroupSerializer}
import play.api.cache.SyncCacheApi
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.{JsResult, JsValue, Json}
import play.api.mvc.Request
import slick.jdbc.PostgresProfile
import slick.lifted.TableQuery
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future
import scala.util.Try

@Singleton
class PlutoWorkingGroupController @Inject() (dbConfigProvider:DatabaseConfigProvider, cacheImpl:SyncCacheApi)
  extends GenericDatabaseObjectController[PlutoWorkingGroup] with PlutoWorkingGroupSerializer {

  implicit val db = dbConfigProvider.get[PostgresProfile].db
  implicit val cache:SyncCacheApi = cacheImpl

  override def selectall(startAt: Int, limit: Int): Future[Try[Seq[PlutoWorkingGroup]]] = db.run(
    TableQuery[PlutoWorkingGroupRow].result.asTry
  )

  override def selectid(requestedId: Int): Future[Try[Seq[PlutoWorkingGroup]]] = db.run(
    TableQuery[PlutoWorkingGroupRow].filter(_.id===requestedId).result.asTry
  )

  override def insert(entry: PlutoWorkingGroup, uid: String): Future[Try[Int]] = throw new RuntimeException("This is not supported")

  override def deleteid(requestedId: Int):Future[Try[Int]] = throw new RuntimeException("This is not supported")

  override def dbupdate(itemId: Int, entry:PlutoWorkingGroup):Future[Try[Int]] = throw new RuntimeException("This is not supported")

  /*these are handled through implict translation*/
  override def jstranslate(result:Seq[PlutoWorkingGroup]):Json.JsValueWrapper = result
  override def jstranslate(result:PlutoWorkingGroup):Json.JsValueWrapper = result

  override def validate(request: Request[JsValue]): JsResult[PlutoWorkingGroup] = request.body.validate[PlutoWorkingGroup]

}
