package services

import java.util.concurrent.TimeUnit

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, StatusCode}
import models.messages.{NewProjectCreated, NewProjectCreatedSerializer}
import play.api.Configuration
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

trait ListenProjectCreate extends Redisson with NewProjectCreatedSerializer with JsonComms {
  val configuration: Configuration

  /**
    * queue listener for project create queue
    * @param queuename name of the queue to listen to
    * @return a Future that does not return
    */
  def listenProjectCreate(queuename:String)(implicit ec:ExecutionContext):Future[Unit] = Future {
    implicit val client = getRedissonClient
    val q = client.getBlockingDeque[String](queuename)
    val enq = client.getBlockingQueue[String](queuename)

    logger.info(s"Setting up queue listener for $queuename")

    while(true){
      val msg = q.pollFirst(60, TimeUnit.SECONDS)
      logger.info(s"Got message for new asset folder: ${msg.toString}")

      Json.fromJson[NewProjectCreated](Json.parse(msg)).asEither match {
        case Right(msgAsObject) =>
          logger.debug(s"Project created message to send: $msgAsObject")
          sendProjectCreatedMessage(msgAsObject).map({
            case Right(_) =>
              logger.info(s"Updated pluto with new project ${msgAsObject.projectEntry.projectTitle} (${msgAsObject.projectEntry.id})")
            case Left(true) =>
              logger.debug("requeueing message after 1s delay")
              queueMessage(queuename, msgAsObject,Some(1.seconds))
            case Left(false) =>
              logger.error("Not retrying any more.")
          }).recoverWith({
            case err:Throwable=>
              logger.error("Could not set up communication with pluto:", err)
              logger.debug("requeueing message after 1s delay")
              queueMessage(queuename, msgAsObject, Some(1.seconds))
          })
        case Left(validationErrors)=>
          logger.error(s"could not deserialize message from queue: $validationErrors")
      }
    }
  }

  def sendProjectCreatedMessage(msg: NewProjectCreated)(implicit ec:ExecutionContext):Future[Either[Boolean,Unit]] = {
    val notifyUrl =  s"${configuration.get[String]("pluto.server_url")}/projects/api/notify"
    val bodyContent:String = Json.toJson(msg).toString()
    logger.debug(s"Going to send json: $bodyContent to $notifyUrl")

    Http().singleRequest(HttpRequest(method=HttpMethods.POST, uri = notifyUrl, headers = List(getPlutoAuth)).withEntity(bodyContent)).map(response=> {
      if (response.status == StatusCode.int2StatusCode(200) || response.status == StatusCode.int2StatusCode(201)) {
        logger.info("Pluto responded success")
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
