package services

import java.net.URLEncoder
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import javax.inject.{Inject, Singleton}
import org.slf4j.MDC
import akka.actor.{Actor, ActorSystem}
import akka.http.scaladsl.{Http, HttpExt}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.stream.ActorMaterializer
import models.{PlutoCommission, PlutoWorkingGroup}
import play.api.db.slick.DatabaseConfigProvider
import play.api.{Configuration, Logger}
import slick.jdbc.PostgresProfile

import scala.util.{Failure, Success, Try}

object PlutoWGCommissionScanner {
  trait ScannerMsg

  case object RefreshWorkingGroups
  case class RefreshCommissionsForWG(workingGroup: PlutoWorkingGroup)
  case class RefreshCommissionsInfo(workingGroup:PlutoWorkingGroup, forSite:String, sinceParam:String, startAt:Int, pageSize: Int)
}

@Singleton
class PlutoWGCommissionScanner @Inject() (playConfig:Configuration, actorSystemI:ActorSystem, dbConfigProvider: DatabaseConfigProvider)
  extends Actor with PlutoWGCommissionScannerFunctions  {

  import PlutoWGCommissionScanner._

  implicit val actorSystem = actorSystemI
  implicit val configuration = playConfig
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = actorSystem.dispatcher
  protected val logger = Logger(getClass)

  implicit val db = dbConfigProvider.get[PostgresProfile].db

  protected val ownRef = self

  protected def getHttp:HttpExt = Http()

  override def receive: Receive = {
    case RefreshWorkingGroups=>
      val originalSender = sender()
      configuration.getOptional[String]("pluto.sync_enabled") match {
        case Some ("yes") =>
          logger.debug("Sync enabled, refreshing working groups")
          refreshWorkingGroups.onComplete({
            case Failure(error)=>
              logger.error(s"Unable to get working groups from server: $error")
              originalSender ! akka.actor.Status.Failure(error)
            case Success(_)=>
              logger.info(s"Completed refreshing working groups")
              originalSender ! akka.actor.Status.Success
          })
        case Some(_)=>
          logger.warn("pluto sync is not enabled. set pluto.sync_enabled to 'yes' in order to enable it")
        case None=>
          logger.error("pluto sync is not enabled")
      }

    case RefreshCommissionsForWG(workingGroup)=>
      val originalSender = sender()
      workingGroup.id match {
        case Some(workingGroupId)=>
          PlutoCommission.mostRecentByWorkingGroup(workingGroupId).onComplete({
            case Failure(error)=>
              logger.error(s"Could not get most recent commissions for $workingGroup", error)
              originalSender ! akka.actor.Status.Failure(error)
            case Success(Failure(error))=>
              logger.error(s"Could not get most recent commissions for $workingGroup", error)
              originalSender ! akka.actor.Status.Failure(error)
            case Success(Success(maybeCommission))=>
              logger.info(s"most recent commission: $maybeCommission")
              val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:MM:ss.SSSZ").withZone(ZoneId.of("UTC"))

              val sinceParam = maybeCommission match {
                case Some(recentCommission)=>
                  logger.info(s"Got most recent commission for $workingGroupId: $recentCommission")
                  s"&since=${URLEncoder.encode(formatter.format(recentCommission.updated.toInstant.plus(java.time.Duration.ofSeconds(1))),"UTF-8")}"
                case None=>
                  logger.info(s"No commissions for $workingGroupId")
                  ""
              }

              //yah, having the site as a config setting is not good but it will do for the time being
              ownRef ! RefreshCommissionsInfo(workingGroup, configuration.get[String]("pluto.sitename"),sinceParam, 0, configuration.get[Int]("pluto.pageSize"))
          })
        case None=>
          logger.error("Can't refresh commissions before working group has been saved.")
          originalSender ! akka.actor.Status.Failure(new RuntimeException("Can't refresh commissions before working group has been saved."))
      }

    case RefreshCommissionsInfo(workingGroup:PlutoWorkingGroup, forSite:String, sinceParam:String, startAt:Int, pageSize: Int)=>
      val authorization = headers.Authorization(BasicHttpCredentials(configuration.get[String]("pluto.username"), configuration.get[String]("pluto.password")))

      val commissionUrl = s"${configuration.get[String]("pluto.server_url")}/commission/api/external/list/?group=${workingGroup.uuid}&start=$startAt&length=$pageSize$sinceParam"
      logger.info(s"refreshing commissions $startAt -> ${startAt + pageSize} for ${workingGroup.name} (${workingGroup.id}) via url $commissionUrl")

      getHttp.singleRequest(HttpRequest(uri = commissionUrl, headers = List(authorization))).flatMap(response => {
        MDC.put("http_status", response.status.toString())
        if (response.status == StatusCodes.OK) {
          bodyAsJsonFuture(response)
        } else if (response.status == StatusCodes.Forbidden || response.status == StatusCodes.Unauthorized) {
          logger.error("Could not log in to server")
          bodyAsJsonFuture(response)
        } else {
          logger.error(s"Server returned ${response.status}")
          bodyAsJsonFuture(response).map({
            //this will cause the Future to fail and is picked up by the caller
            case Right(jsvalue) =>
              Left(s"Could not communicate with pluto: ${jsvalue.toString}")
            case Left(string) =>
              Left(s"Could not communicate with pluto: $string")
          })
        }
      }).flatMap(response=>handleCommissionsInfoData(response, workingGroup, forSite))  //FIXME: iterate over future pages.
  }
}
