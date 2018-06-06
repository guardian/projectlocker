package services.actors.creation

import java.time.ZonedDateTime

import javax.inject.{Inject, Named}
import akka.actor.ActorRef
import exceptions.PostrunActionError
import helpers.PostrunSorter
import models.messages.NewProjectCreated
import models._
import org.slf4j.MDC
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

class RetrievePostruns @Inject() (dbConfigProvider:DatabaseConfigProvider) extends GenericCreationActor {
  override val persistenceId = "creation-retrieve-postruns-actor"

  import GenericCreationActor._
  private implicit val db=dbConfigProvider.get[JdbcProfile].db

  override def receiveCommand: Receive = {
    case createRequest:NewProjectRequest=>
      doPersistedAsync(createRequest) { (msg, originalSender)=>
        Future.sequence(Seq(
          createRequest.rq.projectTemplate.projectType,
          PostrunDependencyGraph.loadAllById
        )).flatMap({ completedFutures=>
          val projectType = completedFutures.head.asInstanceOf[ProjectType]
          val postrunDependencyGraph = completedFutures(1).asInstanceOf[Map[Int,Seq[Int]]]

          projectType.postrunActions.map({
            case Success(actionsList)=>
              val postrunSeq = PostrunSorter.doSort(actionsList.toList, postrunDependencyGraph)
              originalSender ! StepSucceded(updatedData = createRequest.data.copy(postrunSequence = Some(postrunSeq)))
              Success(s"Loaded ${postrunSeq.length} postruns")
            case Failure(error)=>
              originalSender ! StepFailed(createRequest.data, error)
              Success(error.toString)
          })
        })
      }
    case rollbackRequest:NewProjectRollback=>
      logger.debug("No rollback necessary for this actor")
      sender() ! StepSucceded(updatedData = rollbackRequest.data.copy(postrunSequence = None))
    case _=>
      super.receiveCommand
  }
}
