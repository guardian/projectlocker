package services

import akka.actor.ActorRef
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.{HttpRequest, StatusCodes, headers}
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import models.{PlutoCommission, PlutoCommissionSerializer, PlutoWorkingGroup, PlutoWorkingGroupSerializer}
import org.slf4j.MDC
import play.api.Logger
import play.api.libs.json.JsValue
import services.PlutoWGCommissionScanner.RefreshCommissionsForWG
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait PlutoWGCommissionScannerFunctions extends JsonComms with PlutoCommissionSerializer with PlutoWorkingGroupSerializer{
  protected val logger:Logger
  protected val ownRef:ActorRef
  protected def getHttp:HttpExt

  protected def handleCommissionsInfoData(maybeData:Either[String, JsValue], workingGroup:PlutoWorkingGroup, forSite:String)(implicit ec:ExecutionContext, db:JdbcProfile#Backend#Database) = maybeData match {
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

  def receivedWorkingGroupData(workingGroups:List[PlutoWorkingGroup])(implicit ec:ExecutionContext, db:JdbcProfile#Backend#Database) = {
    logger.debug(s"Got working group list")
    MDC.put("working_group_list",workingGroups.toString())
    Future.sequence(workingGroups.map(wg=>{
      logger.debug(s"\t$wg")
      val saveFuture = wg.ensureRecorded
      saveFuture.onComplete({
        case Success(updatedWg)=>
          ownRef ! RefreshCommissionsForWG(updatedWg)

        case Failure(error)=>
          MDC.put("working_group",wg.toString)
          logger.error(s"Unable to save working group to database: ", error)
      })
      saveFuture
    }))
  }

  def refreshWorkingGroups(implicit ec:ExecutionContext, db:JdbcProfile#Backend#Database) = {
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
          receivedWorkingGroupData(parsedData.as[List[PlutoWorkingGroup]])
        case Left(unparsedData)=>
          MDC.put("body",unparsedData)
          logger.error(s"Unable to parse data from server")
          throw new RuntimeException(s"Could not parse data from server, got $unparsedData")
      }
    })
  }

}
