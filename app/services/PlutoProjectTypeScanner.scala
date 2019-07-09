package services

import javax.inject.{Inject, Singleton}
import org.slf4j.MDC
import akka.actor.{Actor, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.stream.ActorMaterializer
import models._
import play.api.db.slick.DatabaseConfigProvider
import play.api.{Configuration, Logger}
import slick.jdbc.PostgresProfile

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._

object PlutoProjectTypeScanner {
  trait PPTMsg

  case object RefreshProjectTypes
  case object RefreshProjectSubtypes
}

class PlutoProjectTypeScanner @Inject() (playConfig:Configuration, actorSystemI:ActorSystem, dbConfigProvider: DatabaseConfigProvider)
  extends Actor with JsonComms with PlutoProjectTypeSerializer  {
  import PlutoProjectTypeScanner._

  implicit val actorSystem = actorSystemI
  implicit val configuration = playConfig
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = actorSystem.dispatcher
  protected val logger = Logger(getClass)

  implicit val db = dbConfigProvider.get[PostgresProfile].db

  protected val ownRef = self

  def onlyWithSyncEnabled[A](block: =>A):Option[A] =
    configuration.getOptional[String]("pluto.sync_enabled") match {
      case Some("yes")=>Some(block)
      case Some(_)=>
        logger.warn("pluto sync is not enabled. set pluto.sync_enabled to 'yes' in order to enable it")
        None
      case None=>
        logger.error("pluto sync is not enbled")
        None
    }

  def handleReceivedTypesList(typeList:List[PlutoProjectType]) = {
    logger.debug(s"Got project type list: $typeList")

    Future.sequence(typeList.map(_.ensureRecorded)).map(savesList=>{
      val failures = savesList.collect({case Failure(err)=>err})
      if(failures.nonEmpty){
        logger.error(s"Could not save all working groups: ")
        failures.foreach(err=>logger.error("\t", err))
      }
      val successes = savesList.collect({case Success(wg)=>wg})

      if(successes.isEmpty){
        logger.error(s"No saves succeeded")
        Left(s"Could not save any working groups. ${failures.length} failed.")
      } else {
        logger.debug(s"Saved ${successes.length} project type records, commencing subtype refresh")
        ownRef ! RefreshProjectSubtypes
      }
    })
  }

  override def receive: Receive = {
    case RefreshProjectTypes=>
      val originalSender = sender()
      logger.info("Rescanning project types...")
      onlyWithSyncEnabled {
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
                handleReceivedTypesList(typeList)
              case Left(unparsedData)=>
                Failure(new RuntimeException(s"Could not parse data from server, got $unparsedData"))
            }
          case Failure(error)=>
            logger.error(s"Unable to get working groups from server: $error")
            originalSender ! akka.actor.Status.Failure(error)
        })
      }

    case RefreshProjectSubtypes=>
      val originalSender = sender()
      onlyWithSyncEnabled {
        logger.info("Refreshing project subtypes")
        val projectTypeUri = s"${configuration.get[String]("pluto.server_url")}/commission/api/project-subtype/"
        MDC.put("project_subtype_uri", projectTypeUri)
        logger.debug(s"project subtype uri is $projectTypeUri")

        val authorization = headers.Authorization(BasicHttpCredentials(configuration.get[String]("pluto.username"), configuration.get[String]("pluto.password")))

        Http().singleRequest(HttpRequest(uri = projectTypeUri, headers = List(authorization))).flatMap(response => {
          MDC.put("http_status", response.status.toString())
          if (response.status == StatusCodes.OK) {
            bodyAsJsonFuture(response)
          } else if (response.status == StatusCodes.Forbidden || response.status == StatusCodes.Unauthorized) {
            logger.error("Could not log in to server")
            bodyAsJsonFuture(response)
          } else {
            logger.error(s"Server returned ${response.status}")
            response.entity.discardBytes()
            throw new RuntimeException("Could not communicate with pluto")
          }
        }).flatMap({
          case Right(parsedData) =>
            MDC.put("response_body", parsedData.toString)
            val typeList = parsedData.as[List[IncomingProjectSubtype]]
            MDC.put("project_subtypes", typeList.toString)
            logger.debug(s"Got ${typeList.length} subtypes to check")
            val resultFuture = Future.sequence(typeList.map(subtype => {
              logger.debug(s"\t$subtype")

              subtype.toPlutoProjectType.map({
                case Some(plutoProjectType) =>
                  plutoProjectType.ensureRecorded.onComplete({
                    case Success(Success(savedType)) =>
                      logger.debug(s"successfully ensured $plutoProjectType is saved")
                    case Success(Failure(error)) =>
                      logger.error(s"Could not save $plutoProjectType: ", error)
                    case Failure(error) =>
                      logger.error(s"Could not save $plutoProjectType: ", error)
                  })
                case None =>
                  logger.error(s"Could not save $subtype because we have no record of a pluto project type named ${subtype.parent_name}")
              })
            }))

            resultFuture.onComplete({
              case Success(_)=>logger.info(s"Completed refresh project subtypes")
              case Failure(err)=>logger.error(s"Could not refresh project subtypes", err)
            })

            resultFuture
          case Left(unparsedData) =>
            MDC.put("unparsed_data", unparsedData)
            Future.failed(new RuntimeException(s"Could not parse data from server, got $unparsedData"))
        }).recover({
          case err:Throwable =>
            logger.error(s"Unable to get working groups from server: ", err)
            originalSender ! err
        })
      }
  }


}