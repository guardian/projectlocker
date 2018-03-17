package services

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.model.{HttpResponse, headers}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import play.api.{Configuration, Logger}
import play.api.libs.json.{JsValue, Json}
import slick.jdbc.PostgresProfile

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

trait JsonComms {
  protected val logger:Logger
  implicit val actorSystem:ActorSystem
  implicit val materializer:ActorMaterializer

  implicit val configuration:Configuration

  protected def getPlutoAuth = headers.Authorization(BasicHttpCredentials(configuration.get[String]("pluto.username"),configuration.get[String]("pluto.password")))

  protected def handlePlutoResponse(response:HttpResponse)(implicit ec:ExecutionContext):Either[Boolean,Unit] = {
    if (response.status.intValue()>=200 && response.status.intValue() <= 299) {
      response.entity.discardBytes()
      logger.info("Pluto responded syccess")
      Right(Unit)
    } else if (response.status.intValue()>=500 && response.status.intValue()<=599) {
      logger.error("Unable to update pluto, server returned 50x error.")
      val body = Await.result(bodyAsJsonFuture(response),5.seconds)
      body match {
        case Left(unparsed)=>
          logger.warn("Could not parse server response, full response is shown as DEBUG message")
          logger.debug(unparsed)
        case Right(parsed)=>
          logger.info(parsed.toString())
      }
      Left(true) //should retrty
    } else {
      logger.error(s"Unable to update pluto, server returned ${response.status}. Not retrying.")
      response.entity.discardBytes()
      Left(false)
    }
  }

  protected def bodyAsJsonFuture(response:HttpResponse)(implicit ec:ExecutionContext):Future[Either[String, JsValue]] = {
    val sink = Sink.fold[String, ByteString]("")(_ + _.decodeString("UTF-8"))

    response.entity.dataBytes.runWith(sink)
      .map(dataString=>
        try {
          Right(Json.parse(dataString))
        } catch {
          case error:Throwable=>
            logger.error(s"Could not decode json object: ", error)
            Left(dataString)
        }
      )
  }
}
