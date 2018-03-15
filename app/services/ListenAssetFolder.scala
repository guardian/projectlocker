package services

import java.util.concurrent.TimeUnit

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, StatusCode}
import models.ProjectEntry
import models.messages.{NewAssetFolder, NewAssetFolderSerializer}
import play.api.Configuration
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.concurrent.duration._

trait ListenAssetFolder extends Redisson with NewAssetFolderSerializer with JsonComms{
  val configuration:Configuration

  /**
    * queue listener for asset folder
    * @param queuename name of the queue to listen
    * @return a Future that does not terminate.
    */
  def listenAssetFolder(queuename:String)(implicit ec:ExecutionContext, db: slick.jdbc.JdbcBackend#DatabaseDef):Future[Unit] = Future {
    implicit val client = getRedissonClient
    val q = client.getBlockingDeque[String](queuename)
    val enq = client.getBlockingQueue[String](queuename)

    logger.info(s"Setting up queue listener for $queuename")

    while(true){
      val msg = q.pollFirst(60, TimeUnit.SECONDS)
      logger.info(s"Got message for new asset folder: ${msg.toString}")

      Json.fromJson[NewAssetFolder](Json.parse(msg)).asEither match {
        case Right(msgAsObject) =>
          getPlutoProjectForAssetFolder(msgAsObject).map({
            case Left(errormessage) =>
              logger.error(s"Could not prepare asset folder message for ${msgAsObject.assetFolderPath} to be sent: $errormessage, pushing it to the back of the queue")
              queueMessage(queuename,msgAsObject,Some(1.seconds)) //put the message to the back of the queue to retry
            case Right(updatedMessage) =>
              logger.debug(s"Updated asset folder message to send: $updatedMessage")
              sendNewAssetFolderMessage(updatedMessage).map({
                case Right(_) =>
                  logger.info(s"Updated pluto with new asset folder ${msgAsObject.assetFolderPath} for ${msgAsObject.plutoProjectId.get}")
                case Left(true) =>
                  logger.debug("requeueing message after 1s delay")
                  queueMessage(queuename, updatedMessage,Some(1.seconds))
                case Left(false) =>
                  logger.error("Not retrying any more.")
              }).recoverWith({
                case err:Throwable=>
                  logger.error("Could not set up communication with pluto:", err)
                  logger.debug("requeueing message after 1s delay")
                  queueMessage(queuename, updatedMessage, Some(1.seconds))
              })
          })
        case Left(validationErrors)=>
          logger.error(s"could not deserialize message from queue: $validationErrors")
      }
    }
  }

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

    Http().singleRequest(HttpRequest(method=HttpMethods.POST, uri = notifyUrl, headers = List(getPlutoAuth)).withEntity(bodyContent)).map(response=> {
      if (response.status == StatusCode.int2StatusCode(200) || response.status == StatusCode.int2StatusCode(201)) {
        logger.info("Pluto responded syccess")
        Right(Unit)
      } else if (response.status == StatusCode.int2StatusCode(500) || response.status == StatusCode.int2StatusCode(503)) {
        logger.error("Unable to update pluto, server returned 500/503 error.")
        Left(true) //should retrty
      } else {
        logger.error(s"Unable to update pluto, server returned ${response.status}. Not retrying.")
        Left(false)
      }
    })
  }
}
