package services

import java.net.URLEncoder
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalUnit
import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString
import helpers.DirectoryScanner
import models._
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.{JsValue, Json}
import play.api.{Configuration, Logger}
import slick.jdbc.PostgresProfile

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._

class PlutoProjectTypeScanner @Inject() (playConfig:Configuration, actorSystemI:ActorSystem, dbConfigProvider: DatabaseConfigProvider)
  extends JsonComms with PlutoProjectTypeSerializer  {
  implicit val actorSystem = actorSystemI
  implicit val configuration = playConfig
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = actorSystem.dispatcher
  protected val logger = Logger(getClass)

  implicit val db = dbConfigProvider.get[PostgresProfile].db

  protected def refreshProjectSubtypes:Unit = {
    configuration.getOptional[String]("pluto.sync_enabled") match {
      case Some("yes")=>
        val projectTypeUri = s"${configuration.get[String]("pluto.server_url")}/commission/api/project-subtype/"
        logger.debug(s"project subtype uri is $projectTypeUri")

        val authorization = headers.Authorization(BasicHttpCredentials(configuration.get[String]("pluto.username"),configuration.get[String]("pluto.password")))

        Http().singleRequest(HttpRequest(uri = projectTypeUri, headers = List(authorization))).flatMap(response=>{
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
                val typeList = parsedData.as[List[IncomingProjectSubtype]]
                logger.debug(s"Got project subtype list:")
                typeList.foreach(subtype=>{
                  logger.debug(s"\t$subtype")

                  subtype.toPlutoProjectType.map({
                    case Some(plutoProjectType)=>
                      plutoProjectType.ensureRecorded.onComplete({
                        case Success(Success(savedType))=>
                          logger.debug(s"successfully ensured ${plutoProjectType} is saved")
                        case Success(Failure(error))=>
                          logger.error(s"Could not save $plutoProjectType: ", error)
                        case Failure(error)=>
                          logger.error(s"Could not save $plutoProjectType: ", error)
                      })
                    case None=>
                      logger.error(s"Could not save $subtype because we have no record of a pluto project type named ${subtype.parent_name}")
                  })
                })
              case Left(unparsedData)=>
                Failure(new RuntimeException(s"Could not parse data from server, got $unparsedData"))
            }
          case Failure(error)=>logger.error(s"Unable to get working groups from server: $error")
        })
      case Some(_)=>
        logger.warn("pluto sync is not enabled. set pluto.sync_enabled to 'yes' in order to enable it")
      case None=>
        logger.error("pluto sync is not enbled")
    }
  }

  protected def refreshProjectTypes:Unit = {
    configuration.getOptional[String]("pluto.sync_enabled") match {
      case Some("yes")=>
        val projectTypeUri = s"${configuration.get[String]("pluto.server_url")}/commission/api/project-type/"
        logger.debug(s"project type uri is $projectTypeUri")

        val authorization = headers.Authorization(BasicHttpCredentials(configuration.get[String]("pluto.username"),configuration.get[String]("pluto.password")))

        Http().singleRequest(HttpRequest(uri = projectTypeUri, headers = List(authorization))).flatMap(response=>{
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
                val typeList = parsedData.as[List[PlutoProjectType]]
                logger.debug(s"Got project type list:")
                typeList.foreach(wg=>{
                  logger.debug(s"\t$wg")
                  Await.result(wg.ensureRecorded,10.seconds) match {
                    case Success(updatedWg)=>
                      logger.debug("")
                    case Failure(error)=>
                      logger.error(s"Unable to save working group to database: ", error)
                  }
                })
                refreshProjectSubtypes
              case Left(unparsedData)=>
                Failure(new RuntimeException(s"Could not parse data from server, got $unparsedData"))
            }
          case Failure(error)=>logger.error(s"Unable to get working groups from server: $error")
        })
      case Some(_)=>
        logger.warn("pluto sync is not enabled. set pluto.sync_enabled to 'yes' in order to enable it")
      case None=>
        logger.error("pluto sync is not enbled")
    }
  }

  val cancellable = actorSystem.scheduler.schedule(5 seconds, 300 seconds) {
    logger.info("Rescanning project types")

    refreshProjectTypes
  }
}