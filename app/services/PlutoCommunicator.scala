package services

import java.util.concurrent.{Executors, TimeUnit}
import javax.inject.{Inject, Singleton}

import models.messages.{NamedQueues, NewAssetFolder, NewAssetFolderSerializer}
import org.redisson.client.codec.StringCodec
import play.api.{Configuration, Logger}

import scala.concurrent.{ExecutionContext, Future}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString
import models.ProjectEntry
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.{JsObject, Json}
import slick.jdbc.PostgresProfile

import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._

@Singleton
class PlutoCommunicator @Inject()(playConfig:Configuration, actorSystemI: ActorSystem, dbConfigProvider:DatabaseConfigProvider)
  extends Redisson with ListenAssetFolder with ListenProjectCreate {

  val logger = Logger(getClass)
  val config = playConfig
  implicit val actorSystem = actorSystemI
  implicit val materializer = ActorMaterializer()
  //implicit val executionContext = actorSystem.dispatcher
  implicit val configuration = playConfig
  implicit val db = dbConfigProvider.get[PostgresProfile].db

  logger.info("PlutoCommunicator")
  //set up our own thread pool, so we can't flood the global one
  val threadPoolSize:Int = playConfig.getOptional[Int]("pluto.communicator_threads").getOrElse(6)

  implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(threadPoolSize))

  listenTest(NamedQueues.TEST)
  listenProjectCreate(NamedQueues.PROJECT_CREATE)
  listenAssetFolder(NamedQueues.ASSET_FOLDER)

  def listenTest(queuename:String):Future[Unit] = Future {
    val client = getRedissonClient
    val q = client.getBlockingDeque[String](queuename)
    logger.info(s"Setting up queue listener for $queuename")

    while(true){
      logger.debug("message loop")
      val msg = q.pollFirst(60, TimeUnit.SECONDS)
      logger.info(s"Got message: ${msg.toString}")
    }
  }

}
