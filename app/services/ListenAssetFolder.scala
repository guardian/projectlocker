package services

import java.util.concurrent.TimeUnit

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import models.ProjectEntry
import models.messages.{NewAssetFolder, NewAssetFolderSerializer}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.Headers

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.concurrent.duration._

trait ListenAssetFolder extends NewAssetFolderSerializer with JsonComms{
  /**
    * ensures that the provided message has the pluto project ID field set.
    * @param msg incoming message.
    * @return if the incoming message has the pluto project ID field set, then a Future containing a Right containing the provided message.
    *         if the incoming message does not have the pluto project ID field set, then looks the project up in the database from the
    *         internal ID number and populates with the pluto project ID if present and returns the new object in a Right
    *         if the project does not have the pluto project ID set then returns a Left with a descriptive error string.
    */
  def getPlutoProjectForAssetFolder(msg: NewAssetFolder)(implicit ec:ExecutionContext, db: slick.jdbc.JdbcBackend#DatabaseDef):Future[Either[String, NewAssetFolder]] = msg.plutoProjectId match {
    case None=> //we still need to get hold of the project reference
      ProjectEntry.entryForId(msg.projectLockerProjectId.get).map({
        case Success(projectEntry:ProjectEntry)=>NewAssetFolder.forCreatedProject(msg.assetFolderPath, projectEntry)
        case Failure(error)=>
          logger.error(s"Could not look up project entry for ${msg.projectLockerProjectId.get}: ", error)
          Left(error.toString)
      })
    case Some(existingProjectReference)=> //we have already got a project reference, so we can go with it
      Future(Right(msg))
  }

  /**
    * sends the given message on to the asset folder notify endpoint in pluto.
    * @param msg NewAssetFolder object representing message to send
    * @return a Future, containing either a Left indicating that the sending failed with a Boolean indicating whether to retry,
    *         or a Right with the Unit value indicating that the sending succeeded
    */
  def sendNewAssetFolderMessage(msg: NewAssetFolder)(implicit ec:ExecutionContext):Future[Either[Boolean, Unit]] = {
    val notifyUrl =  s"${configuration.get[String]("pluto.server_url")}/gnm_asset_folder/api/notify"
    val bodyContent:String = Json.toJson(msg).toString()
    logger.debug(s"Going to send json: $bodyContent to $notifyUrl")

    Http()
      .singleRequest(HttpRequest(method=HttpMethods.POST, uri = notifyUrl, headers = List(getPlutoAuth))
      .withEntity(HttpEntity(ContentType(MediaTypes.`application/json`),bodyContent)))
      .map(handlePlutoResponse)
      .map({
        case Left(retry)=>Left(retry)
        case Right(parsedResponse)=>
          Right(logger.debug("."))
      })
  }
}
