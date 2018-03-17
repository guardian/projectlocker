package services.actors

import com.google.inject.Inject
import akka.actor.{Actor, ActorSystem, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.stream.ActorMaterializer
import models.messages.{NewAssetFolder, NewProjectCreated}
import play.api.{Configuration, Logger}
import services.{ListenAssetFolder, ListenProjectCreate}
import akka.persistence._
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.PostgresProfile

import scala.concurrent.Future
import scala.concurrent.duration._

object MessageProcessorActor {
  def props = Props[MessageProcessorActor]

}

class MessageProcessorActor @Inject()(configurationI: Configuration, actorSystemI: ActorSystem, dbConfigProvider:DatabaseConfigProvider) extends Actor
  with ListenAssetFolder with ListenProjectCreate {
  import akka.cluster.pubsub.DistributedPubSubMediator.Put
  val mediator = DistributedPubSub(context.system).mediator

  mediator ! Put(self)
  import MessageProcessorActor._

  import context.dispatcher

  implicit val configuration = configurationI
  implicit val actorSystem = actorSystemI
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val db = dbConfigProvider.get[PostgresProfile].db

  val logger = Logger(getClass)

  override def receive: Receive = {
    case msgAsObject: NewAssetFolder =>
      val d = durationToPair(Duration(configuration.getOptional[String]("pluto.resend_delay").getOrElse("10 seconds")))
      val delay = FiniteDuration(d._1,d._2)
      logger.info(s"Got new asset folder message: $msgAsObject")
      getPlutoProjectForAssetFolder(msgAsObject).map({
        case Left(errormessage) =>
          logger.error(s"Could not prepare asset folder message for ${msgAsObject.assetFolderPath} to be sent: $errormessage, pushing it to the back of the queue")
          actorSystem.scheduler.scheduleOnce(delay, self, msgAsObject)
        case Right(updatedMessage) =>
          logger.debug(s"Updated asset folder message to send: $updatedMessage")
          sendNewAssetFolderMessage(updatedMessage).map({
            case Right(_) =>
              logger.info(s"Updated pluto with new asset folder ${msgAsObject.assetFolderPath} for ${msgAsObject.plutoProjectId.get}")
            case Left(true) =>
              logger.debug(s"requeueing message after $delay delay")
              actorSystem.scheduler.scheduleOnce(delay, self, msgAsObject)
            case Left(false) =>
              logger.error("Not retrying any more.")
          })
      })

    case msgAsObject:NewProjectCreated =>
        val d = durationToPair(Duration(configuration.getOptional[String]("pluto.resend_delay").getOrElse("10 seconds")))
        val delay = FiniteDuration(d._1,d._2)
        logger.debug(s"Project created message to send: $msgAsObject")
        sendProjectCreatedMessage(msgAsObject).map({
          case Right(_) =>
            logger.info(s"Updated pluto with new project ${msgAsObject.projectEntry.projectTitle} (${msgAsObject.projectEntry.id})")
          case Left(true) =>
            logger.debug(s"requeueing message after $delay delay")
            actorSystem.scheduler.scheduleOnce(delay, self, msgAsObject)
          case Left(false) =>
            logger.error("Not retrying any more.")
        }).recoverWith({
          case err:Throwable=>
            logger.error("Could not set up communication with pluto:", err)
            logger.debug(s"requeueing message after $delay delay")
            Future(actorSystem.scheduler.scheduleOnce(delay, self, msgAsObject))
        })

  }
}
