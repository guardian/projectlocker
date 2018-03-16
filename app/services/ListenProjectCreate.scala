package services

import java.util.concurrent.TimeUnit

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, StatusCode}
import models.messages.{NewProjectCreated, NewProjectCreatedSerializer}
import play.api.Configuration
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

trait ListenProjectCreate extends NewProjectCreatedSerializer with JsonComms {
  def sendProjectCreatedMessage(msg: NewProjectCreated)(implicit ec:ExecutionContext):Future[Either[Boolean,Unit]] = {
    val notifyUrl =  s"${configuration.get[String]("pluto.server_url")}/project/api/external/notifycreated/"
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
