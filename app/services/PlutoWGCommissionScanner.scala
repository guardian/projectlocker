package services

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString
import helpers.DirectoryScanner
import models.{PlutoCommission, PlutoCommissionSerializer, PlutoWorkingGroup, PlutoWorkingGroupSerializer}
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.{JsValue, Json}
import play.api.{Configuration, Logger}
import slick.jdbc.JdbcProfile

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._

@Singleton
class PlutoWGCommissionScanner @Inject() (configuration:Configuration, actorSystem:ActorSystem, dbConfigProvider: DatabaseConfigProvider)
  extends PlutoCommissionSerializer with PlutoWorkingGroupSerializer {
  implicit val systemImplicit = actorSystem
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = actorSystem.dispatcher

  private val logger = Logger(getClass)

  implicit val db = dbConfigProvider.get[JdbcProfile].db

  private def bodyAsJsonFuture(response:HttpResponse):Future[Either[String, JsValue]] = {
    val sink = Sink.fold[String, ByteString]("")(_ + _.decodeString("UTF-8"))

    response.entity.dataBytes.runWith(sink)
      .map(dataString=>
        try {
          Right(Json.parse(dataString))
        } catch {
          case error:Throwable=>
            logger.error(s"Could not decode json object: ", error)
            Left(dataString)
        }
      )
  }

  protected def refreshCommissionsInfo(commissionUrl:String):Future[Try[Int]] = {
    val authorization = headers.Authorization(BasicHttpCredentials(configuration.get[String]("pluto.username"),configuration.get[String]("pluto.password")))

    Http().singleRequest(HttpRequest(uri = commissionUrl, headers = List(authorization))).flatMap(response=>{
      if(response.status==StatusCodes.OK){
        bodyAsJsonFuture(response)
      } else if(response.status==StatusCodes.Forbidden || response.status==StatusCodes.Unauthorized) {
        logger.error("Could not log in to server")
        bodyAsJsonFuture(response)
      }  else {
        logger.error(s"Server returned ${response.status}")
        bodyAsJsonFuture(response).map({
          //this will cause the Future to fail and is picked up by the caller
          case Right(jsvalue)=>
            throw new RuntimeException(s"Could not communicate with pluto: ${jsvalue.toString}")
          case Left(string)=>
            throw new RuntimeException(s"Could not communicate with pluto: $string")
        })
      }
    }).map({
      case Right(parsedData)=>
        logger.debug(s"Received $parsedData from server")
          val commissionList = parsedData.as[List[PlutoCommission]]
          logger.debug(s"Got commission list:")
          Success(commissionList.foldLeft[Int](0)((acc, comm)=>{
              logger.debug(s"\t$comm")
              Await.result(comm.ensureRecorded,10.seconds)
              acc+1
            }))
      case Left(string)=>
        val msg = s"Could not parse response from server: $string"
        logger.error(msg)
        Failure(new RuntimeException(msg))
    })
  }

  def refreshCommissionsForWg(workingGroup: PlutoWorkingGroup):Future[Try[Int]] = {
    val authorization = headers.Authorization(BasicHttpCredentials(configuration.get[String]("pluto.username"),configuration.get[String]("pluto.password")))

    workingGroup.id match {
      case Some(workingGroupId)=>
        PlutoCommission.mostRecentByWorkingGroup(workingGroupId).flatMap({
          case Failure(error)=>Future(Failure(error))
          case Success(maybeCommission)=>
            val sinceParam = maybeCommission match {
              case Some(recentCommission)=>
                s"?since=${recentCommission.updated.toString}"
              case None=>
                ""
            }
            //FIXME: this url is probably wrong
            val commissionUrl = s"${configuration.get[String]("pluto.server_url")}/commission/api/list/${workingGroup.uuid}?since=$sinceParam"
            logger.debug(s"refreshing commissions for ${workingGroup.name} (${workingGroup.uuid}) via url $commissionUrl")
            refreshCommissionsInfo(commissionUrl)
        })
      case None=>
        logger.error("Can't refresh commissions before working group has been saved.")
        Future(Failure(new RuntimeException("Can't refresh commissions before working group has been saved.")))
    }
  }

  def refreshWorkingGroups = {
    configuration.getOptional[String]("pluto.sync_enabled") match {
      case Some("yes")=>
        val workingGroupUri = s"${configuration.get[String]("pluto.server_url")}/commission/api/groups/"
        logger.debug(s"working group uri is $workingGroupUri")

        val authorization = headers.Authorization(BasicHttpCredentials(configuration.get[String]("pluto.username"),configuration.get[String]("pluto.password")))

        Http().singleRequest(HttpRequest(uri = workingGroupUri, headers = List(authorization))).flatMap(response=>{
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
        }).onComplete({
          case Success(parseResult)=>
            logger.debug(s"Received $parseResult from server")
            parseResult match {
              case Right(parsedData)=>
                val wgList = parsedData.as[List[PlutoWorkingGroup]]
                logger.debug(s"Got working group list:")
                wgList.foreach(wg=>{
                  logger.debug(s"\t$wg")
                  Await.result(wg.ensureRecorded,10.seconds) match {
                    case Success(updatedWg)=>
                      refreshCommissionsForWg(updatedWg).onComplete({
                        case Success(triedInt)=> //the future completed ok
                          triedInt match {
                            case Success(commissionsUpdated)=> //the db operation completed ok
                              logger.info(s"Successfully updated ${commissionsUpdated} commissions for working group ${wg.name} (${wg.uuid})")
                            case Failure(error)=>
                              logger.error(s"Database error updating commissions for working group ${wg.name} (${wg.uuid}):", error)
                          }
                        case Failure(error)=>
                          logger.error(s"Unable to update commissions for working group ${wg.name} (${wg.uuid}):", error)
                      })
                    case Failure(error)=>
                      logger.error(s"Unable to save working group to database: ", error)
                  }

                })
            }
          case Failure(error)=>logger.error(s"Unable to get working groups from server: $error")
        })
      case None=>
        logger.error("pluto sync is not enbled")
    }

  }

  val cancellable = actorSystem.scheduler.schedule(1 second,60 seconds) {
    logger.info("Rescanning working groups")

    refreshWorkingGroups
  }
}
