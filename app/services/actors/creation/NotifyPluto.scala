package services.actors.creation

import java.time.ZonedDateTime

import javax.inject.{Inject, Named}
import akka.actor.ActorRef
import models._
import models.messages.NewProjectCreated
import play.api.Configuration
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.duration.Duration
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

class NotifyPluto @Inject() (@Named("message-processor-actor") messageProcessor:ActorRef,
                                 dbConfigProvider:DatabaseConfigProvider,
                                 config:Configuration) extends GenericCreationActor {
  override val persistenceId = "postrun-executor-actor"
  implicit val timeout: Duration = Duration(config.getOptional[String]("postrun.timeout").getOrElse("30 seconds"))

  import GenericCreationActor._

  private implicit val db = dbConfigProvider.get[JdbcProfile].db

  override def receiveCommand: Receive = {
    case sendRequest:NewProjectRequest=>
      doPersistedAsync(sendRequest) { (msg, originalSender)=>
        sendRequest.rq.commissionId match {
          case Some(plutoCommissionId) =>
            PlutoCommission.entryForId(plutoCommissionId).map({
              case Some(plutoCommission)=>
                sendRequest.rq.projectTemplate.projectType.flatMap(_.forPluto(sendRequest.rq.projectTemplate)).onComplete({
                  case Success(projectType) =>
                    messageProcessor ! NewProjectCreated(sendRequest.data.createdProjectEntry.get, projectType, plutoCommission, ZonedDateTime.now().toEpochSecond)
                    originalSender ! StepSucceded(sendRequest.data)
                    Success("Asked message processor to notify")
                  case Failure(error)=>
                    logger.error(s"Could not retrieve pluto project type for ${sendRequest.rq.projectTemplate.projectType}", error)
                    originalSender ! StepFailed(sendRequest.data, error)
                    Success("Unrecoverable error")
                })
              case None=>
                logger.error(s"Could not sync project ${sendRequest.rq} to Pluto as the commission id $plutoCommissionId is not valid")
                originalSender ! StepFailed(sendRequest.data, new RuntimeException("Invalid commission id"))
                Success("Unrecoverable error")
            }).recover({
              case error:Throwable=>
                logger.error(s"Could not retrieve pluto commission object for commission id $plutoCommissionId in ${sendRequest.rq}", error)
                Failure(error) //this should get retried
            })
          case None =>
            val msg=s"Could not sync project ${sendRequest.rq} to Pluto as it has no commission id set"
            logger.error(msg)
            originalSender ! StepFailed(sendRequest.data, new RuntimeException(msg))
            Future(Success(msg))
        }
      }
    case rollbackRequest:NewProjectRequest=>
      logger.error("Rollback has not been implemented for NotifyPluto yet")
  }
}