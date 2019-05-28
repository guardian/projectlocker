package controllers

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import auth.Security
import javax.inject.Inject
import models.{PlutoCommissionSerializer, PlutoWorkingGroup, PlutoWorkingGroupRow}
import play.api.Configuration
import play.api.cache.SyncCacheApi
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.Json
import play.api.mvc.{InjectedController, Result}
import services.PlutoWGCommissionScanner
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._
import scala.util.{Failure, Success}

class PlutoSyncCheckController @Inject()(configuration:Configuration, scanner:PlutoWGCommissionScanner,
                                         dbConfigProvider:DatabaseConfigProvider,override val cache:SyncCacheApi)
                                        (implicit system:ActorSystem)
  extends InjectedController with PlutoCommissionSerializer with Security {

  implicit val db=dbConfigProvider.get[PostgresProfile].db

  def getHttpAuthFromConfig = headers.Authorization(BasicHttpCredentials(configuration.get[String]("pluto.username"),configuration.get[String]("pluto.password")))

  def withWorkingGroup(workingGroupId:Int)(block: PlutoWorkingGroup=>Future[Result]) = db.run(
    TableQuery[PlutoWorkingGroupRow].filter(_.id===workingGroupId).result.asTry
  ).flatMap({
    case Success(workingGroupRows)=>
      workingGroupRows.headOption match {
        case Some(workingGroupRef)=>block(workingGroupRef)
        case None=>
          Future(NotFound(Json.obj("status"->"not_found", "working_group_id"->workingGroupId)))
      }
    case Failure(err)=>
      logger.error("Could not look up working group: ", err)
      Future(InternalServerError(Json.obj("status"->"error", "detail"->"Could not look up working group")))
  })

  def getPlutoCommsForWg(workingGroupId:Int) = IsAdminAsync {uid=>{ request=>
    withWorkingGroup(workingGroupId) { workingGroup =>
      val siteName = configuration.get[String]("pluto.sitename")

      val commissionUrl = s"${configuration.get[String]("pluto.server_url")}/commission/api/external/list/?group=${workingGroup.uuid}"
      val result = scanner.requestData(commissionUrl,getHttpAuthFromConfig)
        .map(_.map(jsData => scanner.commissionListFromParsedData(jsData, workingGroupId,siteName)))

      result.map({
        case Right(commissionList) =>
          Ok(Json.obj("status"->"ok", "commissionList"->commissionList))
        case Left(err) =>
          logger.error(s"HTTP communication failed: $err")
          InternalServerError(Json.obj("status" -> "error", "detail" -> s"HTTP communication failed: $err"))
      }).recover({
        case err: Throwable =>
          logger.error(s"Could not look up pluto commissions for workgroup $workingGroupId: ", err)
          InternalServerError(Json.obj("status" -> "error", "detail" -> err.toString))
      })
    }
  }}

}
