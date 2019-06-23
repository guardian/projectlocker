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
import models.{PlutoCommission, PlutoCommissionSerializer, PlutoWorkingGroup, PlutoWorkingGroupSerializer}
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.{JsValue, Json}
import play.api.{Configuration, Logger}
import slick.jdbc.PostgresProfile

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._

object PlutoWGCommissionScanner {
  trait ScannerMsg

  case object RefreshWorkingGroups
  case class RefreshCommissionsForWG(workingGroup: PlutoWorkingGroup)
  case class RefreshCommissionsInfo(workingGroup:PlutoWorkingGroup, forSite:String, sinceParam:String, startAt:Int, pageSize: Int)
}

@Singleton
class PlutoWGCommissionScanner @Inject() (playConfig:Configuration, actorSystemI:ActorSystem, dbConfigProvider: DatabaseConfigProvider)
  extends Actor with JsonComms with PlutoCommissionSerializer with PlutoWorkingGroupSerializer {

  import PlutoWGCommissionScanner._

  implicit val actorSystem = actorSystemI
  implicit val configuration = playConfig
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = actorSystem.dispatcher
  protected val logger = Logger(getClass)

  implicit val db = dbConfigProvider.get[PostgresProfile].db

  protected val ownRef = self

  protected def getHttp:HttpExt = Http()

  protected def handleCommissionsInfoData(maybeData:Either[String, JsValue], workingGroup:PlutoWorkingGroup, forSite:String) = maybeData match {
    case Right(parsedData)=>
      MDC.put("body", parsedData.toString())
      logger.debug(s"Received $parsedData from server")
      val commissionList = parsedData.as[List[JsValue]]
        .map(PlutoCommission.fromServerRepresentation(_, workingGroup.id.get, forSite))
        .collect({
          case Success(comm) => comm
        })
      MDC.put("commission_list", commissionList.toString())
      val ensureFutures = Future.sequence(commissionList.map(_.ensureRecorded))

  //    commissionList.foldLeft[Int](0)((acc, comm) => {
  //      logger.debug(s"\t$comm")
  ////      Await.result(comm.ensureRecorded, 10.seconds)
  ////      if (acc >= pageSize - 1) refreshCommissionsInfo(workingGroup, forSite, sinceParam, startAt + acc, pageSize)
  //      acc + 1
  //    })
      ensureFutures.map(resultList=>{
        val failures = resultList.collect({case Failure(err)=>err})
        if(failures.nonEmpty){
          logger.error(s"Could not save all collections: ")
          failures.foreach(err=>logger.error(s"\tFailed commission:", err))
          Left(failures.mkString("\n"))
        } else {
          Right(resultList.length)
        }
      })
    case Left(err)=>Future(Left(err))
  }

  def receivedWorkingGroupData(parsedData:JsValue) = {
    val wgList = parsedData.as[List[PlutoWorkingGroup]]
    logger.debug(s"Got working group list:")
    MDC.put("working_group_list",wgList.toString())
    Future.sequence(wgList.map(wg=>{
      logger.debug(s"\t$wg")
      val saveFuture = wg.ensureRecorded
      saveFuture.onComplete({
        case Success(updatedWg)=>
          ownRef ! RefreshCommissionsForWG(updatedWg)

//          refreshCommissionsForWg(updatedWg).onComplete({
//            case Success(triedInt)=> //the future completed ok
//              triedInt match {
//                case Success(commissionsUpdated)=> //the db operation completed ok
//                  logger.info(s"Successfully updated $commissionsUpdated commissions for working group ${wg.name} (${wg.uuid})")
//                case Failure(error)=>
//                  MDC.put("working_group",wg.toString)
//                  logger.error(s"Database error updating commissions for working group ${wg.name} (${wg.uuid}):", error)
//              }
//            case Failure(error)=>
//              MDC.put("working_group",wg.toString)
//              logger.error(s"Unable to update commissions for working group ${wg.name} (${wg.uuid}):", error)
//          })
        case Failure(error)=>
          MDC.put("working_group",wg.toString)
          logger.error(s"Unable to save working group to database: ", error)
      })
      saveFuture
    }))
  }

  def refreshWorkingGroups = {
    logger.debug("in refreshWorkingGroups")
    val workingGroupUri = s"${configuration.get[String]("pluto.server_url")}/commission/api/groups/"
    logger.debug(s"working group uri is $workingGroupUri")

    val authorization = headers.Authorization(BasicHttpCredentials(configuration.get[String]("pluto.username"),configuration.get[String]("pluto.password")))

    getHttp.singleRequest(HttpRequest(uri = workingGroupUri, headers = List(authorization))).flatMap(response=>{
      MDC.put("http_status",response.status.toString())
      if(response.status==StatusCodes.OK){
        bodyAsJsonFuture(response)
      } else if(response.status==StatusCodes.Forbidden || response.status==StatusCodes.Unauthorized) {
        logger.error("Could not log in to server")
        bodyAsJsonFuture(response)
      }  else {
        logger.error(s"Server returned ${response.status}")
        response.entity.discardBytes()
        throw new RuntimeException("Could not communicate with pluto")
      }
    }).flatMap(parseResult=>{
        MDC.put("body",parseResult.toString)
        logger.debug(s"Received $parseResult from server")
        parseResult match {
          case Right(parsedData)=>
            receivedWorkingGroupData(parsedData)
          case Left(unparsedData)=>
            MDC.put("body",unparsedData)
            logger.error(s"Unable to parse data from server")
            throw new RuntimeException(s"Could not parse data from server, got $unparsedData")
        }
    })
  }

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
//              refreshCommissionsInfo(workingGroup, configuration.get[String]("pluto.sitename"),sinceParam, 0, configuration.get[Int]("pluto.pageSize")).onComplete({
//                case Success(Right(count))=>
//                  logger.info(s"Refreshed $count items from $workingGroup")
//                  originalSender ! akka.actor.Status.Success
//                case Success(Left(err))=>
//                  val msg = s"Could not scan commissions from $workingGroup: $err"
//                  logger.error(msg)
//                  originalSender ! akka.actor.Status.Failure(new RuntimeException(msg))
//                case Failure(err)=>
//                  val msg = s"Refresh thread crashed: "
//                  logger.error(msg, err)
//                  originalSender ! akka.actor.Status.Failure(err)
//              })
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
      }).flatMap(response=>handleCommissionsInfoData(response, workingGroup, forSite))
  }
}
