package services.actors

import com.google.inject.Inject
import akka.actor.{Actor, ActorSystem, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.stream.ActorMaterializer
import models.messages.{NewAdobeUuid, NewAssetFolder, NewProjectCreated}
import play.api.{Configuration, Logger}
import services.{ListenAssetFolder, ListenNewUuid, ListenProjectCreate}
import akka.persistence._
import models.ProjectEntry
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.PostgresProfile

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object MessageProcessorActor {
  def props = Props[MessageProcessorActor]

}

class MessageProcessorActor @Inject()(configurationI: Configuration, actorSystemI: ActorSystem, dbConfigProvider:DatabaseConfigProvider) extends Actor
  with ListenAssetFolder with ListenProjectCreate with ListenNewUuid {
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

    case msgAsObject:NewAdobeUuid =>
      val d = durationToPair(Duration(configuration.getOptional[String]("pluto.resend_delay").getOrElse("10 seconds")))
      val delay = FiniteDuration(d._1,d._2)
      logger.info("Informing pluto of updated adobe uuid")
      logger.debug(s"Update uuid message to send: $msgAsObject")

      msgAsObject.projectEntry.vidispineProjectId match {
        case None=>
          logger.warn(s"Can't update project ${msgAsObject.projectEntry.id} in Pluto without a vidispine ID. Retrying after delay")
          ProjectEntry.entryForId(msgAsObject.projectEntry.id.get).map({
            case Failure(error)=>
              logger.error("Could not update project entry: ", error)
              actorSystem.scheduler.scheduleOnce(delay, self, msgAsObject)
            case Success(updatedEntry)=>
              actorSystem.scheduler.scheduleOnce(delay, self, msgAsObject.copy(projectEntry = updatedEntry))
          })
        case Some(vidispineId)=>
          sendNewUuidMessage(msgAsObject).map({
            case Right(parsedResponse)=>
              logger.info(s"Successfully updated project $vidispineId to have uuid ${msgAsObject.projectAdobeUuid}")
            case Left(true)=>
              logger.debug(s"Requeing message after $delay delay")
              actorSystem.scheduler.scheduleOnce(delay, self, msgAsObject)
            case Left(false)=>
              logger.error("Not retrying any more.")
          })
      }
  }
}
