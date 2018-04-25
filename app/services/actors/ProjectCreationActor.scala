package services.actors

import akka.actor.{ActorRef, Props}
import akka.pattern.ask
import models.ProjectRequestFull
import play.api.Logger
import services.actors.creation.GenericCreationActor.{NewProjectRequest, NewProjectRollback, StepFailed, StepSucceded}
import services.actors.creation.{CreateFileEntry, CreationMessage, GenericCreationActor, GetStorageDriver}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import scala.util.{Failure, Success}

trait CreationRunnerMethods {
  import GenericCreationActor._

  implicit val timeout:akka.util.Timeout = 30.seconds
  protected val logger:Logger


}

class ProjectCreationActor extends GenericCreationActor with CreationRunnerMethods {
  override val persistenceId = "project-creation-actor"

  import GenericCreationActor._

  /**
    * Runs the next actor in the given sequence recursively by sending it [[NewProjectRequest]].
    * If it fails, then [[NewProjectRollback]] is sent to it and the recursion ends; as the recursion unwinds all of the
    * actors that had run [[NewProjectRequest]] get sent [[NewProjectRollback]] to clean themselves up
    * @param actorSequence sequence of actorRef of the actors to signal
    * @param rq [[ProjectRequestFull]] object representing the project that we want to create
    * @return a Future of either Left(StepFailed) or Right(StepSucceeded) depending on the reply sent back by the actor
    */
  def runNextActorInSequence(actorSequence:Seq[ActorRef], rq:ProjectRequestFull):Future[Either[StepFailed,StepSucceded]] = {
    if(actorSequence.isEmpty) return Future(Right(StepSucceded()))

    logger.info(actorSequence.head.toString())
    logger.info(actorSequence.toString())

    logger.info(s"i am ${context.self}")
    val resultFuture = actorSequence.head ? NewProjectRequest(rq, None)
    resultFuture.onComplete({ //use this to catch exceptions
      case Success(result)=>logger.info(s"actor ask success: $result")
      case Failure(error)=>logger.info(s"actor ask failure: $error")
    })

    resultFuture.flatMap({
      case successMessage:StepSucceded=>
        logger.info("stepSucceeded")
        runNextActorInSequence(actorSequence.tail, rq) flatMap {
          case Left(failedMessage)=>  //if the _next_ step fails, tell this step to roll back
            (actorSequence.head ? NewProjectRollback(rq)).map(result=>Left(failedMessage))
          case Right(nextActorSuccess)=>
            Future(Right(successMessage))
        }
      case failedMessage:StepFailed=> //if the step fails, tell it to roll back
        logger.info("StepFailed")
        //don't actually care about the result of rollback, but do care about sequencing
        (actorSequence.head ? NewProjectRollback(rq)).map(result=>Left(failedMessage))
      case other:Any=>
        logger.error(s"got unexpected message: ${other.getClass}")
        Future(Left(StepFailed(new RuntimeException("got unexpected message"))))
    })
  }

//  val creationActorChain:Seq[ActorRef] = Seq(
//    CreateFileEntry.getClass,
//    GetStorageDriver.getClass
//  ).map(cls=>context.actorOf(Props(cls)))
  val creationActorChain:Seq[ActorRef] = Seq()

  override def receiveCommand: Receive = {
    case rq:NewProjectRequest=>
      logger.info(s"got request: $rq")
      logger.info(s"i am ${context.self}")
      logger.info(s"sender is ${sender()}")
      val originalSender = sender()
      runNextActorInSequence(creationActorChain, rq.rq).map({
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
