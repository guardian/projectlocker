package services.actors

import akka.actor.{ActorRef, Props}
import akka.pattern.ask
import models.ProjectRequestFull
import play.api.Logger
import services.actors.creation.GenericCreationActor.{NewProjectRequest, NewProjectRollback, StepFailed, StepSucceded}
import services.actors.creation.{CreateFileEntry, CreationMessage, GenericCreationActor, CopySourceFile}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class ProjectCreationActor extends GenericCreationActor {
  override val persistenceId = "project-creation-actor"
  override protected val logger=Logger(getClass)

  import GenericCreationActor._
  implicit val timeout:akka.util.Timeout = 30.seconds

  /**
    * Runs the next actor in the given sequence recursively by sending it [[NewProjectRequest]].
    * If it fails, then [[NewProjectRollback]] is sent to it and the recursion ends; as the recursion unwinds all of the
    * actors that had run [[NewProjectRequest]] get sent [[NewProjectRollback]] to clean themselves up
    * @param actorSequence sequence of actorRef of the actors to signal
    * @param rq [[ProjectRequestFull]] object representing the project that we want to create
    * @return a Future of either Left(StepFailed) or Right(StepSucceeded) depending on the reply sent back by the actor
    */
  def runNextActorInSequence(actorSequence:Seq[ActorRef], rq:ProjectRequestFull, data:ProjectCreateTransientData):Future[Either[StepFailed,StepSucceded]] = {
    if(actorSequence.isEmpty) return Future(Right(StepSucceded(data)))

    val resultFuture = actorSequence.head ? NewProjectRequest(rq, None,data)
    resultFuture.onComplete({ //use this to catch exceptions
      case Success(result)=>logger.info(s"actor ask success: $result")
      case Failure(error)=>logger.info(s"actor ask failure: $error")
    })

    resultFuture.flatMap({
      case successMessage:StepSucceded=>
        logger.debug("stepSucceeded, running next actor in sequence")
        runNextActorInSequence(actorSequence.tail, rq,successMessage.updatedData) flatMap {
          case Left(failedMessage)=>  //if the _next_ step fails, tell this step to roll back
            (actorSequence.head ? NewProjectRollback(rq, successMessage.updatedData)).map(result=>Left(failedMessage))
          case Right(nextActorSuccess)=>
            Future(Right(successMessage))
        }
      case failedMessage:StepFailed=> //if the step fails, tell it to roll back
        logger.warn(s"StepFailed, sending rollback to ${actorSequence.head}")
        //don't actually care about the result of rollback, but do care about sequencing
        (actorSequence.head ? NewProjectRollback(rq, failedMessage.updatedData)).map(result=>Left(failedMessage))
      case other:Any=>
        logger.error(s"got unexpected message: ${other.getClass}")
        Future(Left(StepFailed(data, new RuntimeException("got unexpected message"))))
    })
  }

  val creationActorChain:Seq[ActorRef] = Seq()

  override def receiveCommand: Receive = {
    case rq:NewProjectRequest=>
      logger.info(s"got request: $rq")
      logger.info(s"i am ${context.self}")
      logger.info(s"sender is ${sender()}")
      val originalSender = sender()
      val initialData = ProjectCreateTransientData(None,None,None)
      runNextActorInSequence(creationActorChain, rq.rq, initialData).map({
        case Left(stepFailed)=>
          logger.warn("A subactor failed")
          originalSender ! ProjectCreateFailed(rq.rq)
        case Right(stepSucceded)=>
          logger.info("All subactors succeded")
          originalSender ! ProjectCreateSucceeded(rq.rq)
      })
    case msg:Any=>
      logger.info(s"got other message: ${msg}")
      super.receiveCommand
  }
}
