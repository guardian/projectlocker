package services

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import models.messages.{NewProjectCreated, NewProjectCreatedSerializer}
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

trait ListenProjectCreate extends NewProjectCreatedSerializer with JsonComms {
  def sendProjectCreatedMessage(msg: NewProjectCreated)(implicit ec:ExecutionContext):Future[Either[Boolean,Unit]] = {
    val notifyUrl =  s"${configuration.get[String]("pluto.server_url")}/project/api/external/notifycreated/"
    val bodyContent:String = Json.toJson(msg).toString()
    logger.debug(s"Going to send json: $bodyContent to $notifyUrl")

    Http().singleRequest(HttpRequest(method=HttpMethods.POST, uri = notifyUrl, headers = List(getPlutoAuth))
      .withEntity(HttpEntity(ContentType(MediaTypes.`application/json`),bodyContent)))
      .map(handlePlutoResponse)
  }
}
