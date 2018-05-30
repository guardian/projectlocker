package ProjectCreation
import java.sql.Timestamp
import java.time.{LocalDateTime, ZonedDateTime}

import akka.actor.{ActorRef, ActorSystem, Props}
import org.specs2.mutable._
import akka.testkit._
import models.{FileEntry, ProjectEntry, ProjectRequest, ProjectRequestFull}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import services.actors.ProjectCreationActor
import services.actors.creation.GenericCreationActor._
import akka.pattern.ask
import play.api.test.WithApplication
import utils.BuildMyApp

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Try}
import scala.concurrent.ExecutionContext.Implicits.global

class ProjectCreationSpec extends Specification with BuildMyApp {
  private val logger=Logger(getClass)
  implicit val timeout:akka.util.Timeout = 30.seconds

  "runNextActorInSequence" should {
    "run a list of actors providing that they all return successfully" in new WithApplication(buildApp){
      private val injector = app.injector

      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val system = injector.instanceOf(classOf[ActorSystem])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val probe1 = TestProbe()
      val probe2 = TestProbe()
      val probe3 = TestProbe()

      val actorSeq = Seq(probe1.ref,probe2.ref,probe3.ref)
      val ac = system.actorOf(Props(new ProjectCreationActor(system, app) {
        override val creationActorChain: Seq[ActorRef] = Seq(probe1.ref, probe2.ref, probe3.ref)
      }))

      val initialData = ProjectCreateTransientData(None, None, None)

      val rq = Await.result(ProjectRequest("somefile.prj",1,"some test file",1,"testuser",None,None).hydrate,10.seconds)
      rq must beSome

      val resultFuture = ac ? NewProjectRequest(rq.get, None, initialData)
      val timestamp = Timestamp.valueOf("2018-02-21 13:15:16")

      probe1.expectMsg(30.seconds, NewProjectRequest(rq.get, None, initialData))
      logger.info(probe1.lastSender.toString)
      val fakeProject = ProjectEntry(None,1,None,"test title",timestamp,"tst-user",None,None)
      val updatedData = initialData.copy(createdProjectEntry = Some(fakeProject))

      probe1.reply(StepSucceded(updatedData))
      probe2.expectMsg(30.seconds, NewProjectRequest(rq.get, None, updatedData))
      probe2.reply(StepSucceded(updatedData))
      probe3.expectMsg(30.seconds, NewProjectRequest(rq.get, None, updatedData))
      probe3.reply(StepSucceded(updatedData))

      val result = Await.result(resultFuture,90.seconds)
      result mustEqual ProjectCreateSucceeded(rq.get, fakeProject)
    }

    "stop when an actor reports a failure and roll back the ones that had run before" in new WithApplication(buildApp){
      private val injector = app.injector

      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val system = injector.instanceOf(classOf[ActorSystem])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val probe1 = TestProbe()
      val probe2 = TestProbe()
      val probe3 = TestProbe()

      val initialData = ProjectCreateTransientData(None, None, None)

      val actorSeq = Seq(probe1.ref,probe2.ref,probe3.ref)
      val ac = system.actorOf(Props(new ProjectCreationActor(system, app) {
        override val creationActorChain: Seq[ActorRef] = Seq(probe1.ref, probe2.ref, probe3.ref)
      }))

      val rq = Await.result(ProjectRequest("somefile.prj",1,"some test file",1,"testuser",None,None).hydrate,10.seconds)
      rq must beSome

      val resultFuture = ac ? NewProjectRequest(rq.get, None, initialData)

      val ex=new RuntimeException("My hovercraft is full of eels")

      probe1.expectMsg(5.seconds, NewProjectRequest(rq.get, None, initialData))
      probe1.reply(StepSucceded(initialData))
      probe2.expectMsg(5.seconds, NewProjectRequest(rq.get, None, initialData))
      probe2.reply(StepFailed(initialData, ex))
      probe2.expectMsg(5.seconds, NewProjectRollback(rq.get, initialData))
      probe2.reply(StepSucceded(initialData))
      probe1.expectMsg(5.seconds, NewProjectRollback(rq.get, initialData))
      probe1.reply(StepSucceded(initialData))

      probe3.expectNoMessage(5.seconds)

      val result = Await.result(resultFuture,15.seconds)
      result mustEqual ProjectCreateFailed(rq.get, ex)
    }

    "continue rollback even if a rollback fails" in new WithApplication(buildApp){
      private val injector = app.injector

      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val system = injector.instanceOf(classOf[ActorSystem])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val probe1 = TestProbe()
      val probe2 = TestProbe()
      val probe3 = TestProbe()

      val actorSeq = Seq(probe1.ref,probe2.ref,probe3.ref)
      val ac = system.actorOf(Props(new ProjectCreationActor(system, app) {
        override val creationActorChain: Seq[ActorRef] = Seq(probe1.ref, probe2.ref, probe3.ref)
      }))
      val initialData = ProjectCreateTransientData(None, None, None)

      val rq = Await.result(ProjectRequest("somefile.prj",1,"some test file",1,"testuser",None,None).hydrate,10.seconds)
      rq must beSome

      val resultFuture = ac ? NewProjectRequest(rq.get, None, initialData)

      val ex=new RuntimeException("My hovercraft is full of eels")

      probe1.expectMsg(5.seconds, NewProjectRequest(rq.get, None, initialData))
      probe1.reply(StepSucceded(initialData))
      probe2.expectMsg(5.seconds, NewProjectRequest(rq.get, None, initialData))
      probe2.reply(StepFailed(initialData, ex))
      probe2.expectMsg(5.seconds, NewProjectRollback(rq.get, initialData))
      probe2.reply(StepFailed(initialData, ex))
      probe1.expectMsg(5.seconds, NewProjectRollback(rq.get, initialData))
      probe1.reply(StepSucceded(initialData))

      probe3.expectNoMessage(5.seconds)

      val result = Await.result(resultFuture,15.seconds)
      result mustEqual ProjectCreateFailed(rq.get, ex)
    }
  }


}
