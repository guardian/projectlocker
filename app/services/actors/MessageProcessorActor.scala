package services.actors

import java.util.UUID

import com.google.inject.Inject
import akka.actor.{Actor, ActorSystem, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.stream.ActorMaterializer
import models.messages.{NewAdobeUuid, NewAssetFolder, NewProjectCreated, QueuedMessage}
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

  trait MessageEvent {
    val rq: QueuedMessage
    val eventId: UUID
  }

  case class NewProjectCreatedEvent(rq: NewProjectCreated, eventId: UUID) extends MessageEvent
  case class EventHandled(eventId: UUID)
  case class RetryFromState()
}

class MessageProcessorActor @Inject()(configurationI: Configuration, actorSystemI: ActorSystem, dbConfigProvider:DatabaseConfigProvider) extends PersistentActor
  with ListenAssetFolder with ListenProjectCreate with ListenNewUuid {
  override def persistenceId = "message-processor-actor"

  var state = MessageProcessorState()

  import akka.cluster.pubsub.DistributedPubSubMediator.Put
  val mediator = DistributedPubSub(context.system).mediator

  mediator ! Put(self)
  import MessageProcessorActor._

  import context.dispatcher

  implicit val configuration = configurationI
  implicit val actorSystem = actorSystemI
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val db = dbConfigProvider.get[PostgresProfile].db

  protected val snapshotInterval = configuration.getOptional[Long]("pluto.persistence-snapshot-interval").getOrElse(50L)
  val logger = Logger(getClass)

  /**
    * add an event to the journal, and snapshot if required
    * @param event event to add
    */
  def updateState(event:MessageEvent): Unit = {
    state = state.updated(event)
    if(lastSequenceNr % snapshotInterval ==0 && lastSequenceNr!=0)
      saveSnapshot(state)
  }

  /**
    * Logs to the journal that this event has been handled, so it won't be re-tried
    * @param evtAsObject event object
    */
  def confirmHandled(evtAsObject:  MessageEvent):Unit = {
    persist(EventHandled(evtAsObject.eventId)){ handledEventMarker=>
      state = state.removed(evtAsObject)
      saveSnapshot(state)
    }
  }

  override def receiveRecover:Receive = {
    case evt:MessageEvent =>
      logger.debug(s"receiveRecover got message event: $evt")
      updateState(evt)
    case handledEvt:EventHandled =>
      logger.debug(s"receiveRecover got message handled: ${handledEvt.eventId}")
      state = state.removed(handledEvt.eventId)
    case RecoveryCompleted=>
      logger.info("MessageProcessorActor completed journal recovery, starting automatic retries")
      actorSystem.scheduler.schedule(FiniteDuration(30,"seconds"),FiniteDuration(30,"seconds"),self,RetryFromState())
    case SnapshotOffer(_, snapshot: MessageProcessorState)=>
      logger.debug("receiveRecover got snapshot offer")
      state=snapshot
  }

  override def receiveCommand: Receive = {
    case SaveSnapshotSuccess(metadata)=>
      logger.debug(s"Successfully saved snapshot: $metadata")
      logger.debug(s"Now removing messages to sequence no ${metadata.sequenceNr} from journal")
      deleteMessages(metadata.sequenceNr)
    case SaveSnapshotFailure(metadata,error)=>
      logger.error(s"Could not save snapshot ${metadata.sequenceNr} for ${metadata.persistenceId}: ",error)
    case retry: RetryFromState=>  //retry all events in journal
      logger.debug("initiating retry cycle")
      state.foreach{ self ! _._2 }

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
      persist(NewProjectCreatedEvent(msgAsObject, UUID.randomUUID())) { event =>
        updateState(event)
        logger.debug("persisted new created event to journal, now sending")
        self ! event
      }

    case evtAsObject:NewProjectCreatedEvent =>
      logger.debug("received new project created event")
      val msgAsObject = evtAsObject.rq
      val d = durationToPair(Duration(configuration.getOptional[String]("pluto.resend_delay").getOrElse("10 seconds")))
      val delay = FiniteDuration(d._1, d._2)
      logger.debug(s"Project created message to send: $msgAsObject")
      sendProjectCreatedMessage(msgAsObject).map({
        case Right(_) =>
          logger.info(s"Updated pluto with new project ${msgAsObject.projectEntry.projectTitle} (${msgAsObject.projectEntry.id})")
          confirmHandled(evtAsObject)
        case Left(true) =>
          logger.debug(s"will retry from state ")
        case Left(false) =>
          logger.error("Not retrying any more.")
          confirmHandled(evtAsObject)
      }).recoverWith({
        case err: Throwable =>
          logger.error("Could not set up communication with pluto:", err)
          Future(logger.debug(s"message will be requeued after $delay delay"))
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
