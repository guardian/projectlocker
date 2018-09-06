package services.actors

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.UUID

import akka.actor.{ActorSystem, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.persistence._
import akka.stream.ActorMaterializer
import com.amazonaws.ClientConfigurationFactory
import com.amazonaws.auth.{AWSCredentialsProvider, AWSStaticCredentialsProvider, STSAssumeRoleSessionCredentialsProvider}
import com.amazonaws.services.kinesis.model.PutRecordRequest
import com.amazonaws.services.kinesis.{AmazonKinesis, AmazonKinesisClient, AmazonKinesisClientBuilder}
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceAsyncClientBuilder
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest
import com.google.inject.Inject
import models.{MediaAtomProjectSerializer, ProjectEntry}
import models.messages.{NewAssetFolder, QueuedMessage}
import org.slf4j.MDC
import play.api.{Configuration, Logger}
import play.api.db.slick.DatabaseConfigProvider
import services.actors.MessageProcessorActor.{EventHandled, MessageEvent, NewAssetFolderEvent, RetryFromState}
import slick.jdbc.PostgresProfile

import scala.concurrent.duration.{Duration, FiniteDuration, durationToPair}
import scala.util.{Failure, Success, Try}

object MediaAtomUpdateActor {
  def props:Props = Props[MediaAtomUpdateActor]

  trait MediaAtomEvent {
    val rq: MediaAtomMessage
    val eventId: UUID
  }

  trait MediaAtomMessage

  case class ProjectCreated(projectlockerId: Int, plutoId: String) extends MediaAtomMessage
  case class ProjectUpdated(projectlockerId: Int, plutoId: String) extends MediaAtomMessage

  case class ProjectCreatedEvent(rq: ProjectCreated, eventId: UUID) extends MediaAtomEvent
  case class ProjectUpdatedEvent(rq: ProjectUpdated, eventId: UUID) extends MediaAtomEvent

}

class MediaAtomUpdateActor @Inject()(configurationI: Configuration, actorSystemI: ActorSystem, dbConfigProvider:DatabaseConfigProvider)
  extends PersistentActor with MediaAtomProjectSerializer {
  private final val logger = Logger(getClass)

  override def persistenceId: String = "media-atom-update-actor"

  var state = MediaAtomUpdateState()

  import akka.cluster.pubsub.DistributedPubSubMediator.Put
  val mediator = DistributedPubSub(context.system).mediator

  implicit val configuration = configurationI
  implicit val actorSystem = actorSystemI
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val db = dbConfigProvider.get[PostgresProfile].db

  protected val snapshotInterval = configuration.getOptional[Long]("pluto.persistence-snapshot-interval").getOrElse(50L)

  mediator ! Put(self)
  import MediaAtomUpdateActor._

  import context.dispatcher

  /**
    * add an event to the journal, and snapshot if required
    * @param event event to add
    */
  def updateState(event:MediaAtomEvent): Unit = {
    logger.debug(s"Marked event ${event.eventId} as pending (${event.rq})")
    state = state.updated(event)
    if(lastSequenceNr % snapshotInterval ==0 && lastSequenceNr!=0)
      saveSnapshot(state)
  }

  /**
    * Logs to the journal that this event has been handled, so it won't be re-tried
    * @param evtAsObject event object
    */
  def confirmHandled(evtAsObject:  MediaAtomEvent):Unit = {
    persist(EventHandled(evtAsObject.eventId)){ handledEventMarker=>
      logger.debug(s"marked event ${evtAsObject.eventId} as handled")
      state = state.removed(evtAsObject)
    }
  }

  override def receiveRecover:Receive = {
    case evt:MediaAtomEvent =>
      logger.debug(s"receiveRecover got message event: $evt")
      updateState(evt)
    case handledEvt:EventHandled =>
      logger.debug(s"receiveRecover got message handled: ${handledEvt.eventId}")
      state = state.removed(handledEvt.eventId)
    case RecoveryCompleted=>
      val d = durationToPair(Duration(configuration.getOptional[String]("pluto.resend_delay").getOrElse("60 seconds")))
      val delay = FiniteDuration(d._1,d._2)
      logger.info(s"MessageProcessorActor completed journal recovery, starting automatic retries every $delay")
      actorSystem.scheduler.schedule(delay, delay,self,RetryFromState())
    case SnapshotOffer(_, snapshot: MediaAtomUpdateState)=>
      logger.debug("receiveRecover got snapshot offer")
      state=snapshot
  }

  protected def getKinesisClient():Try[AmazonKinesis] = {
    //use the default credential provider chain to get the creds for authenticating to STS
    val stsClient = AWSSecurityTokenServiceAsyncClientBuilder.standard().build()

    configuration.getOptional[String]("mediaAtom.roleArn") match {
      case Some(roleArn) =>
        Try {
          val provider = new STSAssumeRoleSessionCredentialsProvider.Builder(roleArn, "projectlocker-media-atom-integration").build()

          AmazonKinesisClientBuilder.standard().withCredentials(provider).build()
        }
      case None=>
        Failure(new RuntimeException("No value was set for mediaAtom.roleArn in the configuration"))
    }
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

    case msgAsObject: ProjectCreated =>
      persist(ProjectCreatedEvent(msgAsObject, UUID.randomUUID())) { event=>
        updateState(event)
        logger.debug("persisted project created to journal, now sending")
        self ! event
      }

    case evtAsObject: ProjectCreatedEvent =>
      getKinesisClient() match {
        case Success(kinesisClient)=>
          MDC.put("project-id", evtAsObject.rq.projectlockerId.toString)
          MDC.put("pluto-project-id", evtAsObject.rq.plutoId)
          MDC.put("msg-uuid", evtAsObject.eventId.toString)

          ProjectEntry.entryForId(evtAsObject.rq.projectlockerId).map({
            case Success(projectEntry)=>
              val streamName = configuration.get[String]("mediaAtom.kinesisStream")
              logger.info(s"Updating media atom with info about project ID ${evtAsObject.rq.projectlockerId} via stream $streamName")
              projectEntry.getMediaAtomMessage("project-created").map({
                case Success(msg)=>
                  val jsonString = mediaAtomProjectWrites.writes(msg).toString()
                  logger.debug(s"json to send: $jsonString")

                  kinesisClient.putRecord(new PutRecordRequest()
                    .withStreamName(streamName)
                    .withData(ByteBuffer.wrap(jsonString.getBytes(Charset.forName("UTF-8"))))
                    .withPartitionKey(msg.id)
                  )
                case Failure(err)=>
                  logger.error(s"Could not get media atom message for project ${evtAsObject.rq.projectlockerId}", err)
              })

              confirmHandled(evtAsObject)
            case Failure(error)=>
              logger.error(s"Could not look up project ID ${evtAsObject.rq.projectlockerId} in the database:", error)
          })
          MDC.remove("project-id")
          MDC.remove("pluto-project-id")
          MDC.remove("msg-uuid")
        case Failure(error)=>
          logger.error(s"Could not get kinesis client to update media atom for project ID ${evtAsObject.rq.projectlockerId} ", error)
      }
  }


}
