package controllers
import akka.actor.ActorRef
import javax.inject._
import models.{PlutoWorkingGroup, PlutoWorkingGroupRow, PlutoWorkingGroupSerializer}
import play.api.cache.SyncCacheApi
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.{JsResult, JsValue, Json}
import play.api.mvc.{ControllerComponents, Request}
import slick.jdbc.PostgresProfile
import slick.lifted.TableQuery
import slick.jdbc.PostgresProfile.api._
import akka.pattern.ask
import auth.BearerTokenAuth
import services.PlutoWGCommissionScanner

import scala.concurrent.Future
import scala.util.Try
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

@Singleton
class PlutoWorkingGroupController @Inject() (override val controllerComponents:ControllerComponents, override val bearerTokenAuth:BearerTokenAuth,
                                             dbConfigProvider:DatabaseConfigProvider, cacheImpl:SyncCacheApi, @Named("pluto-wg-commission-scanner") scanner:ActorRef)
  extends GenericDatabaseObjectController[PlutoWorkingGroup] with PlutoWorkingGroupSerializer {

  implicit val timeout:akka.util.Timeout = 55 seconds

  implicit val db = dbConfigProvider.get[PostgresProfile].db
  implicit val cache:SyncCacheApi = cacheImpl

  override def selectall(startAt: Int, limit: Int): Future[Try[Seq[PlutoWorkingGroup]]] = db.run(
    TableQuery[PlutoWorkingGroupRow].sortBy(_.name.asc.nullsLast).result.asTry
  )

  override def selectid(requestedId: Int): Future[Try[Seq[PlutoWorkingGroup]]] = db.run(
    TableQuery[PlutoWorkingGroupRow].filter(_.id===requestedId).sortBy(_.name.asc.nullsLast).result.asTry
  )

  override def insert(entry: PlutoWorkingGroup, uid: String): Future[Try[Int]] = throw new RuntimeException("This is not supported")

  override def deleteid(requestedId: Int):Future[Try[Int]] = throw new RuntimeException("This is not supported")

  override def dbupdate(itemId: Int, entry:PlutoWorkingGroup):Future[Try[Int]] = throw new RuntimeException("This is not supported")

  /*these are handled through implict translation*/
  override def jstranslate(result:Seq[PlutoWorkingGroup]):Json.JsValueWrapper = result
  override def jstranslate(result:PlutoWorkingGroup):Json.JsValueWrapper = result

  override def validate(request: Request[JsValue]): JsResult[PlutoWorkingGroup] = request.body.validate[PlutoWorkingGroup]

  def rescan = IsAdminAsync { uid=> { request=>
    (scanner ? PlutoWGCommissionScanner.RefreshWorkingGroups).map({
      case akka.actor.Status.Success=>Ok("rescan started")
      case akka.actor.Status.Failure(err)=>
        logger.error(s"Could not start pluto scan: ", err)
        InternalServerError("Could not start scan. See logs for details.")
      case other@_=>
        logger.error(s"Received unexpected reply from RefreshWorkingGroups: ${other.getClass}")
        InternalServerError("Program error, see logs for details")
    }).recover({
      case err:Throwable=>
        logger.error(s"Could not start pluto scan: ", err)
        InternalServerError("Could not start scan. See logs for details.")
    })
  }}
}
