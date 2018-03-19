package services

import models.messages.{NewAdobeUuid, NewAdobeUuidSerializer}
import play.api.libs.json.{JsValue, Json}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._

import scala.concurrent.{ExecutionContext, Future}

trait ListenNewUuid extends NewAdobeUuidSerializer with JsonComms {
  def sendNewUuidMessage(msg: NewAdobeUuid)(implicit ec: ExecutionContext): Future[Either[Boolean, JsValue]] = {
    val notifyUrl =  s"${configuration.get[String]("pluto.server_url")}/project/${msg.projectEntry.vidispineProjectId.get}/api/external/notifyuuid"
    val bodyContent:String = Json.toJson(msg).toString()
    logger.debug(s"Going to send json: $bodyContent to $notifyUrl")

    Http().singleRequest(HttpRequest(method=HttpMethods.POST, uri = notifyUrl, headers = List(getPlutoAuth))
      .withEntity(HttpEntity(ContentType(MediaTypes.`application/json`),bodyContent)))
      .map(handlePlutoResponse)
  }
}
