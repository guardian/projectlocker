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
import models.{PlutoWorkingGroup, PlutoWorkingGroupSerializer}
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.{JsValue, Json}
import play.api.{Configuration, Logger}
import slick.jdbc.JdbcProfile

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}
import scala.concurrent.duration._

@Singleton
class PlutoWGCommissionScanner @Inject() (configuration:Configuration, actorSystem:ActorSystem, dbConfigProvider: DatabaseConfigProvider) extends PlutoWorkingGroupSerializer{
  implicit val systemImplicit = actorSystem
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = actorSystem.dispatcher

  private val logger = Logger(getClass)

  implicit val db = dbConfigProvider.get[JdbcProfile].db

  def bodyAsJsonFuture(futureResponse:Future[HttpResponse]):Future[Either[String, JsValue]] = futureResponse.flatMap(response=>{
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
  })

  def refreshWorkingGroups = {
    configuration.getOptional[String]("pluto.sync_enabled") match {
      case Some("yes")=>
        val workingGroupUri = s"${configuration.get[String]("pluto.server_url")}/commission/api/groups/"
        logger.debug(s"working group uri is $workingGroupUri")

        val authorization = headers.Authorization(BasicHttpCredentials(configuration.get[String]("pluto.username"),configuration.get[String]("pluto.password")))

        Http().singleRequest(HttpRequest(uri = workingGroupUri, headers = List(authorization))).flatMap(response=>{
          if(response.status==StatusCodes.OK){
            bodyAsJsonFuture(Future(response))
          } else if(response.status==StatusCodes.Forbidden || response.status==StatusCodes.Unauthorized) {
            logger.error("Could not log in to server")
            bodyAsJsonFuture(Future(response))
          }  else {
            logger.error(s"Server returned ${response.status}")
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
                  Await.ready(wg.ensureRecorded,10.seconds)
                })
            }
          case Failure(error)=>logger.error(s"erorred: $error")
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
