package services

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import models.{PlutoCommission, ProjectEntry, VidispineId}
import models.messages.{NewCommissionCreated, NewCommissionCreatedSerializer, NewProjectCreated, NewProjectCreatedSerializer}
import play.api.libs.json.{JsValue, Json}
import org.slf4j.MDC

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait ListenProjectCreate extends NewProjectCreatedSerializer with NewCommissionCreatedSerializer with JsonComms {
  def sendProjectCreatedMessage(msg: NewProjectCreated)(implicit ec:ExecutionContext, db: slick.jdbc.JdbcBackend#DatabaseDef):Future[Either[Boolean,Unit]] = {
    val bodyContent:String = Json.toJson(msg).toString()

    val notifyUrl =  s"${configuration.get[String]("pluto.server_url")}/project/api/external/notifycreated/"

    genericSendPlutoMessage(bodyContent,notifyUrl).flatMap({
      case Left(error)=>Future(Left(error))
      case Right(parsedResponse)=>
        try {
          val status = (parsedResponse \ "status").as[String]
          if(status=="in_progress"){
            logger.warn(s"Updating project entry ${msg.projectEntry.id} in Pluto - Pluto claims to already be processing this.")
            (parsedResponse \ "detail").asOpt[String] match {
              case Some(detail)=>logger.warn(s"Pluto said $detail")
              case None=>logger.warn("Pluto provided no details")
            }
            Future(Left(false))
          } else {
            val projectId = (parsedResponse \ "project_id").as[String]
            //get an updated copy of the project entry from the database, as it is possible that a user has updated it in between
            //the creation of the msg object and this getting called
            ProjectEntry.entryForId(msg.projectEntry.id.get).map({
              case Success(updatedProjectEntry) =>
                updatedProjectEntry.copy(vidispineProjectId = Some(projectId)).save
                Right(logger.info(s"Updated project entry id ${msg.projectEntry.id} with vidispine id $projectId"))
              case Failure(error) =>
                logger.error(s"Could not update database with vidispine id $projectId for entry id ${msg.projectEntry.id}", error)
                Left(true)
            })
          }
        } catch {
          case ex: Throwable=>
            logger.error("Got 200 response but no project_id")
            logger.error(s"Response content was ${parsedResponse.toString()}, trace is", ex)
            Future(Left(true))
        }
    })
  }

  def sendCommissionCreatedMessage(msg:NewCommissionCreated)
                                  (implicit ec:ExecutionContext, db: slick.jdbc.JdbcBackend#DatabaseDef):Future[Either[Boolean,Unit]] = {
    msg.commission.toServerRepresentationForCreate.flatMap(jsv=>{
      val bodyContent = jsv.toString()
      val notifyUrl = s"${configuration.get[String]("pluto.server_url")}/commission/api/new"

      genericSendPlutoMessage(bodyContent, notifyUrl).flatMap({
        case Left(error) => Future(Left(error))
        case Right(parsedResponse) =>
          try {
            VidispineId.fromString((parsedResponse \ "commission_id").as[String]) match {
              case Success(vsid) =>
                logger.info(s"Received confirmation from pluto. Updating commission ${msg.commission.id} with provided VS ID ${vsid.toString}")
                val updatedCommission = msg.commission.copy(collectionId = Some(vsid.numericPart), siteId = vsid.siteId)
                updatedCommission.save.map({
                  case Success(savedComm) =>
                    Right()
                  case Failure(err) =>
                    logger.error("Could not save updated commission record", err)
                    Left(true)
                })
              case Failure(err) =>
                logger.error(s"Unable to convert ${(parsedResponse \ "commission_id").as[String]} from Pluto into a Vidispine ID", err)
                Future(Left(true))
            }
          } catch {
            case ex: Throwable =>
              logger.error("Got 200 response but no commission_id")
              logger.error(s"Response content was ${parsedResponse.toString()}, trace is", ex)
              Future(Left(true))
          }
      })
    })
  }

  def genericSendPlutoMessage(bodyContent:String, notifyUrl:String)(implicit ec:ExecutionContext, db: slick.jdbc.JdbcBackend#DatabaseDef):Future[Either[Boolean, JsValue]] ={
    logger.debug(s"Going to send json: $bodyContent to $notifyUrl")

    MDC.put("bodyContent", bodyContent)
    MDC.put("notifyUrl", notifyUrl)
    Http().singleRequest(HttpRequest(method=HttpMethods.POST, uri = notifyUrl, headers = List(getPlutoAuth))
      .withEntity(HttpEntity(ContentType(MediaTypes.`application/json`),bodyContent)))
      .map(handlePlutoResponse)
  }
}
