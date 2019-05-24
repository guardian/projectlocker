package services.actors

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
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
  case class NewAdobeUuidEvent(rq: NewAdobeUuid, eventId: UUID, receivedAt:ZonedDateTime) extends MessageEvent
  case class NewAssetFolderEvent(rq: NewAssetFolder, eventId: UUID, receivedAt:ZonedDateTime) extends MessageEvent

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
    logger.debug(s"Marked event ${event.eventId} as pending (${event.rq})")
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
      logger.debug(s"marked event ${evtAsObject.eventId} as handled")
      state = state.removed(evtAsObject)
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
      val d = durationToPair(Duration(configuration.getOptional[String]("pluto.resend_delay").getOrElse("10 seconds")))
      val delay = FiniteDuration(d._1,d._2)
      logger.info(s"MessageProcessorActor completed journal recovery, starting automatic retries every $delay")
      actorSystem.scheduler.schedule(delay, delay,self,RetryFromState())
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
    case retry: RetryFromState=>  //retry all events in state, i.e. everything that has not had confirmHandle() called
      logger.debug(s"initiating retry cycle, entries in state: ${state.size}")
      state.foreach{ stateEntry=>
        logger.debug(s"${stateEntry._1.toString}: ${stateEntry._2.toString}")
        self ! stateEntry._2
      }

    case msgAsObject: NewAssetFolder =>
      persist(NewAssetFolderEvent(msgAsObject, UUID.randomUUID(), ZonedDateTime.now())) { event=>
        updateState(event)
        logger.debug("persisted new asset folder event to journal, now sending")
        self ! event
      }

    case evtAsObject: NewAssetFolderEvent=>
      logger.info(s"Got new asset folder message: ${evtAsObject.rq}")
      configuration.getOptional[String]("pluto.sync_enabled") match {
        case Some("no") => logger.warn("Not sending asset folder message to pluto as sync_enabled is set to 'no'")
        case _ =>
          getPlutoProjectForAssetFolder(evtAsObject.rq).map({
            case Left(errormessage) =>
              if(evtAsObject.receivedAt.isBefore(ZonedDateTime.now().minus(5, ChronoUnit.DAYS))){
                logger.error(s"Received asset folder message for ${evtAsObject.rq.assetFolderPath} more than 5 days ago, dropping it as it's unlikely to work now")
                confirmHandled(evtAsObject)
              } else {
                logger.error(s"Could not prepare asset folder message for ${evtAsObject.rq.assetFolderPath} to be sent: $errormessage, pushing it to the back of the queue")
              }
            case Right(updatedMessage) =>
              logger.debug(s"Updated asset folder message to send: $updatedMessage")
              sendNewAssetFolderMessage(updatedMessage).map({
                case Right(msgString) =>
                  logger.info(msgString)
                  logger.info(s"Updated pluto with new asset folder ${updatedMessage.assetFolderPath} for ${updatedMessage.plutoProjectId}")
                  confirmHandled(evtAsObject)
                case Left(true) =>
                  logger.debug(s"requeueing message for retry after delay")
                case Left(false) =>
                  logger.error("Not retrying any more.")
                  confirmHandled(evtAsObject)
              })
          })
      }

    case msgAsObject:NewProjectCreated =>
      persist(NewProjectCreatedEvent(msgAsObject, UUID.randomUUID())) { event =>
        updateState(event)
        logger.debug("persisted new created event to journal, now sending")
        self ! event
      }

    case evtAsObject:NewProjectCreatedEvent =>
      logger.debug("received new project created event")
      val msgAsObject = evtAsObject.rq
      configuration.getOptional[String]("pluto.sync_enabled") match {
        case Some("no") =>
          logger.warn("Not sending asset folder message to pluto as sync_enabled is set to 'no'")
          confirmHandled(evtAsObject)
        case _ =>
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
              Future(logger.debug(s"message will be requeued"))
          })
      }

    case msgAsObject:NewAdobeUuid =>
      persist(NewAdobeUuidEvent(msgAsObject, UUID.randomUUID(), ZonedDateTime.now())) { event=>
        updateState(event)
        logger.debug("persisted new adove uuid event to journal, now sending")
        self ! event
      }

    case evtAsObject:NewAdobeUuidEvent =>
      logger.info("Informing pluto of updated adobe uuid")
      logger.debug(s"Update uuid message to send: ${evtAsObject.rq}")

      //most probably, the message that we have been given does not include a vidispine uuid. so, we should look that up here.
      configuration.getOptional[String]("pluto.sync_enabled") match {
        case Some("no") => logger.warn("Not sending asset folder message to pluto as sync_enabled is set to 'no'")
        case _ =>
          ProjectEntry.entryForId(evtAsObject.rq.projectEntry.id.get).map({
            case Failure(error) =>
              logger.error("Could not update project entry (will keep retrying): ", error)
            case Success(updatedEntry) =>
              updatedEntry.vidispineProjectId match {
                case Some(vidispineId) =>
                  sendNewUuidMessage(NewAdobeUuid(updatedEntry, evtAsObject.rq.projectAdobeUuid)).map({
                    case Right(parsedResponse) =>
                      logger.info(s"Successfully updated project $vidispineId to have uuid ${evtAsObject.rq.projectAdobeUuid}")
                      confirmHandled(evtAsObject)
                    case Left(true) =>
                      logger.debug(s"Requeing message")
                    case Left(false) =>
                      logger.error("Not retrying any more.")
                      confirmHandled(evtAsObject)
                  })
                case None =>
                  if(evtAsObject.receivedAt.isBefore(ZonedDateTime.now().minus(5, ChronoUnit.DAYS))){
                    logger.error(s"Received project UUID message for ${evtAsObject.rq.projectEntry.projectTitle} more than 5 days ago, dropping it as it's unlikely to work now")
                    confirmHandled(evtAsObject)
                  } else {
                    logger.warn(s"Can't update project ${updatedEntry.id} in Pluto without a vidispine ID. Retrying after delay")
                  }

              }
          })
      }
  }
}
