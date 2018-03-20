package services

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import models.ProjectEntry
import models.messages.{NewProjectCreated, NewProjectCreatedSerializer}
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait ListenProjectCreate extends NewProjectCreatedSerializer with JsonComms {
  def sendProjectCreatedMessage(msg: NewProjectCreated)(implicit ec:ExecutionContext, db: slick.jdbc.JdbcBackend#DatabaseDef):Future[Either[Boolean,Unit]] = {
    val notifyUrl =  s"${configuration.get[String]("pluto.server_url")}/project/api/external/notifycreated/"
    val bodyContent:String = Json.toJson(msg).toString()
    logger.debug(s"Going to send json: $bodyContent to $notifyUrl")

    Http().singleRequest(HttpRequest(method=HttpMethods.POST, uri = notifyUrl, headers = List(getPlutoAuth))
      .withEntity(HttpEntity(ContentType(MediaTypes.`application/json`),bodyContent)))
      .map(handlePlutoResponse)
      .flatMap({
        case Left(error)=>Future(Left(error))
        case Right(parsedResponse)=>
          try {
            val projectId = (parsedResponse \ "project_id").as[String]
            //get an updated copy of the project entry from the database, as it is possible that a user has updated it in between
            //the creation of the msg object and this getting called
            ProjectEntry.entryForId(msg.projectEntry.id.get).flatMap({
              case Success(updatedProjectEntry)=>
                updatedProjectEntry.copy(vidispineProjectId = Some(projectId)).save.map({
                  case Success(newProjectEntry)=>
                    Right(logger.info(s"Updated project entry id ${newProjectEntry.id} with vidispine id ${newProjectEntry.vidispineProjectId}"))
                  case Failure(error)=>
                    logger.error("Could not update record: ", error)
                    Left(true)
                })
              case Failure(error)=>
                logger.error(s"Could not update database with vidispine id $projectId for entry id ${msg.projectEntry.id}", error)
                Future(Left(true))
            })
          } catch {
            case ex: Throwable=>
              logger.error("Got 200 response but no project_id")
              logger.error(s"Response content was ${parsedResponse.toString()}, trace is", ex)
              Future(Left(true))
          }
      })
  }
}
