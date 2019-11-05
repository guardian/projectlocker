package services.actors

import javax.inject.Inject

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import models.ProjectRequestFull
import org.slf4j.MDC
import play.api.{Application, Logger}
import play.api.inject.Injector
import services.actors.creation.GenericCreationActor.{NewProjectRequest, NewProjectRollback, StepFailed, StepSucceded}
import services.actors.creation._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class ProjectCreationActor @Inject() (system:ActorSystem, app:Application) extends GenericCreationActor {
  override val persistenceId = "project-creation-actor"
  override protected val logger=Logger(getClass)

  import GenericCreationActor._
  implicit val timeout:akka.util.Timeout = 30.seconds

  /**
    * This property defines the step chain to create a project, in terms of actors required.
    * It's overridden in the tests, to create an artificial chain with TestProbes.
    */
  val creationActorChain:Seq[ActorRef] = Seq(
    system.actorOf(Props(app.injector.instanceOf(classOf[CreateFileEntry]))),
    system.actorOf(Props(app.injector.instanceOf(classOf[CopySourceFile]))),
    system.actorOf(Props(app.injector.instanceOf(classOf[CreateProjectEntry]))),
    system.actorOf(Props(app.injector.instanceOf(classOf[RetrievePostruns]))),
    system.actorOf(Props(app.injector.instanceOf(classOf[PostrunExecutor]))),
  )

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
      case Success(result)=>logger.debug(s"actor ask success: $result")
      case Failure(error)=>logger.debug(s"actor ask failure: $error")
    })

    resultFuture.flatMap({
      case successMessage:StepSucceded=>
        logger.debug("stepSucceeded, running next actor in sequence")
        runNextActorInSequence(actorSequence.tail, rq,successMessage.updatedData) flatMap {
          case Left(failedMessage)=>  //if the _next_ step fails, tell this step to roll back
            (actorSequence.head ? NewProjectRollback(rq, successMessage.updatedData)).map(result=>Left(failedMessage))
          case Right(nextActorSuccess)=>
            Future(Right(nextActorSuccess))
        }
      case failedMessage:StepFailed=> //if the step fails, tell it to roll back
        logger.warn(s"StepFailed, sending rollback to ${actorSequence.head}")
        //don't actually care about the result of rollback, but do care about sequencing
        (actorSequence.head ? NewProjectRollback(rq, failedMessage.updatedData)).map(result=>Left(failedMessage))
      case other:Any=>
        logger.error(s"got unexpected message: ${other.getClass}")
        Future(Left(StepFailed(data, new RuntimeException("got unexpected message"))))
    }).recover({
      case err:Throwable=>
        logger.error(s"Actor ask failed for ${actorSequence.head.path.toString}: ", err)
        Left(StepFailed(data, err))
    })
  }

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
          originalSender ! ProjectCreateFailed(rq.rq, stepFailed.err)
        case Right(stepSucceded)=>
          logger.info("All subactors succeded")
          stepSucceded.updatedData.createdProjectEntry match {
            case None=>
              MDC.put("updatedData", stepSucceded.updatedData.toString)
              logger.error("Project creation actors succeeded but no created project entry?")
              originalSender ! ProjectCreateFailed(rq.rq, new RuntimeException("Project creation actors succeeded but no created project entry?"))
            case Some(projectEntry)=>
              originalSender ! ProjectCreateSucceeded(rq.rq, projectEntry)
          }
      })
    case msg:Any=>
      logger.info(s"got other message: ${msg}")
      super.receiveCommand
  }
}
