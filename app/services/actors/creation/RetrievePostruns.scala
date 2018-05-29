package services.actors.creation

import java.time.ZonedDateTime
import javax.inject.{Inject, Named}

import akka.actor.ActorRef
import exceptions.PostrunActionError
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

  def orderPostruns(unodererdList:Seq[PostrunAction],dependencies:Map[Int,Seq[Int]]):Seq[PostrunAction] = unodererdList sortWith { (postrunA,postrunB)=>
    val firstTest = dependencies.get(postrunA.id.get) match {
      case Some(dependencies)=>
        //        if(dependencies.contains(postrunB.id.get))
        //          println(s"'${postrunA.title}' Adeps '${postrunB.title}'")
        //        else
        //          println(s"'${postrunA.title}' not deps '${postrunB.title}'")
        dependencies.contains(postrunB.id.get)  //if A depends on B, then reverse the order
      case None=>
        //        println(s"'${postrunA.runnable}' has no dependencies")
        logger.debug(s"${postrunA.runnable} has no dependencies")
        false
    }

    !firstTest
  }

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
              val postrunSeq = orderPostruns(actionsList, postrunDependencyGraph)
              originalSender ! StepSucceded(updatedData = createRequest.data.copy(postrunSequence = Some(postrunSeq)))
              Success(s"Loaded ${postrunSeq.length} postruns")
            case Failure(error)=>
              originalSender ! StepFailed(createRequest.data, error)
              Failure(error)
          })
        })
      }
    case rollbackRequest:NewProjectRollback=>
      logger.debug("No rollback necessary for this actor")
    case _=>
      super.receiveCommand
  }
}
