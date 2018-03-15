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
  extends JsonComms with Redisson with NewAssetFolderSerializer {

  val logger = Logger(getClass)
  val config = playConfig
  implicit val actorSystem = actorSystemI
  implicit val materializer = ActorMaterializer()
  //implicit val executionContext = actorSystem.dispatcher
  implicit val configuration = playConfig
  implicit val db = dbConfigProvider.get[PostgresProfile].db

  //set up our own thread pool, so we can't flood the global one
  val threadPoolSize:Int = playConfig.getOptional[Int]("pluto.communicator_threads").getOrElse(4)

  implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(threadPoolSize))

  listenTest(NamedQueues.TEST)
  //listen[Nothing](NamedQueues.PROJECT_CREATE)
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

  def listenAssetFolder(queuename:String):Future[Unit] = Future {
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

  def getPlutoProjectForAssetFolder(msg: NewAssetFolder):Future[Either[String, NewAssetFolder]] = msg.plutoProjectId match {
    case None=> //we still need to get hold of the project reference
      ProjectEntry.entryForId(msg.projectLockerProjectId.get).map({
        case Success(projectEntry)=>NewAssetFolder.forCreatedProject(msg.assetFolderPath, projectEntry)
        case Failure(error)=>
          logger.error(s"Could not look up project entry for ${msg.projectLockerProjectId.get}: ", error)
          Left(error.toString)
      })
    case Some(existingProjectReference)=> //we have already got a project reference, so we can go with it
      Future(Right(msg))
  }

  def sendNewAssetFolderMessage(msg: NewAssetFolder):Future[Either[Boolean, Unit]] = {
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
