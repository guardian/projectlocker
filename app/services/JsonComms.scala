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

import scala.concurrent.{ExecutionContext, Future}

trait JsonComms {
  protected val logger:Logger
  implicit val actorSystem:ActorSystem
  implicit val materializer:ActorMaterializer

  implicit val configuration:Configuration

  protected def getPlutoAuth = headers.Authorization(BasicHttpCredentials(configuration.get[String]("pluto.username"),configuration.get[String]("pluto.password")))

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
