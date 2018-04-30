package ProjectCreation
import java.sql.Timestamp
import java.time.{LocalDateTime, ZonedDateTime}

import akka.actor.{ActorRef, ActorSystem, Props}
import org.specs2.mutable._
import akka.testkit._
import models.{FileEntry, ProjectRequest, ProjectRequestFull}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import slick.jdbc.{JdbcBackend, JdbcProfile}
import testHelpers.TestDatabase
import services.actors.ProjectCreationActor
import services.actors.creation.GenericCreationActor._
import akka.pattern.ask
import akka.testkit
import services.actors.creation.{CreateFileEntry, CreationMessage}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Try}
import scala.concurrent.ExecutionContext.Implicits.global

class ProjectCreationSpec extends Specification {
  sequential

  private val logger=Logger(getClass)

  //can over-ride bindings here. see https://www.playframework.com/documentation/2.5.x/ScalaTestingWithGuice
  private val application = new GuiceApplicationBuilder()
    .overrides(bind[DatabaseConfigProvider].to[TestDatabase.testDbProvider])
    .build
  private val injector = application.injector

  private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
  private implicit val system = injector.instanceOf(classOf[ActorSystem])
  private implicit val db = dbConfigProvider.get[JdbcProfile].db

  implicit val timeout:akka.util.Timeout = 30.seconds

  "runNextActorInSequence" should {
    "run a list of actors providing that they all return successfully" in {
      val probe1 = TestProbe()
      val probe2 = TestProbe()
      val probe3 = TestProbe()

      val actorSeq = Seq(probe1.ref,probe2.ref,probe3.ref)
      val ac = system.actorOf(Props(new ProjectCreationActor {
        override val creationActorChain: Seq[ActorRef] = Seq(probe1.ref, probe2.ref, probe3.ref)
      }))
      logger.info(ac.toString)

      val rq = Await.result(ProjectRequest("somefile.prj",1,"some test file",1,"testuser",None,None).hydrate,10.seconds)
      rq must beSome

      val resultFuture = ac ? NewProjectRequest(rq.get, None)

      probe1.expectMsg(30.seconds, NewProjectRequest(rq.get, None))
      logger.info(probe1.lastSender.toString)
      probe1.reply(StepSucceded())
      probe2.expectMsg(30.seconds, NewProjectRequest(rq.get, None))
      probe2.reply(StepSucceded())
      probe3.expectMsg(30.seconds, NewProjectRequest(rq.get, None))
      probe3.reply(StepSucceded())

      1 mustEqual 1

      val result = Await.result(resultFuture,90.seconds)
      result mustEqual ProjectCreateSucceeded(rq.get)
    }
  }

  "CreateFileEntry->NewProjectRequest" should {
    "create a file entry and respond with StepSucceeded" in {
      val ac = system.actorOf(Props(new CreateFileEntry(dbConfigProvider)))

      val fileEntryBefore = Await.result(FileEntry.entryFor("testfile.prproj",1), 2 seconds)
      fileEntryBefore must beSuccessfulTry
      fileEntryBefore.get.length mustEqual 0

      val maybeRq = Await.result(ProjectRequest("testfile",1,"Test project entry", 1, "test-user", None, None).hydrate, 10 seconds)
      maybeRq must beSome

      val dateTime = LocalDateTime.now()
      val msg = NewProjectRequest(maybeRq.get, Some(dateTime))

      val response = Await.result((ac ? msg).mapTo[Either[CreationMessage, CreationMessage]], 10 seconds)
      response must beRight

      val fileEntryAfter = Await.result(FileEntry.entryFor("testfile.prproj",1), 2 seconds)
      fileEntryAfter must beSuccessfulTry
      val entrySeq = fileEntryAfter.get
      entrySeq.length mustEqual 1
      entrySeq.head.hasContent must beFalse
    }

    "return StepFailed with an exception if there is an error" in {
      val ex=new RuntimeException("My hovercraft is full of eels")
      val ac = system.actorOf(Props(new CreateFileEntry(dbConfigProvider) {
        override def getDestFileFor(rq: ProjectRequestFull, recordTimestamp: Timestamp)
                                   (implicit db: JdbcBackend#DatabaseDef): Future[Try[FileEntry]] = Future(Failure(ex))
      }))

      val fileEntryBefore = Await.result(FileEntry.entryFor("testfile2.prproj",1), 2 seconds)
      fileEntryBefore must beSuccessfulTry
      fileEntryBefore.get.length mustEqual 0

      val maybeRq = Await.result(ProjectRequest("testfile2",1,"Test project entry", 1, "test-user", None, None).hydrate, 10 seconds)
      maybeRq must beSome

      val dateTime = LocalDateTime.now()
      val msg = NewProjectRequest(maybeRq.get, Some(dateTime))

      val response = Await.result((ac ? msg).mapTo[Either[StepFailed, StepSucceded]], 10 seconds)
      response must beLeft(StepFailed(ex))

      val fileEntryAfter = Await.result(FileEntry.entryFor("testfile2.prproj",1), 2 seconds)
      fileEntryAfter must beSuccessfulTry
      val entrySeq = fileEntryAfter.get
      entrySeq.length mustEqual 0
    }
  }

  "CreateFileEntry->NewProjectRollback" should {
    "delete an existing file entry" in {
      val ac = system.actorOf(Props(new CreateFileEntry(dbConfigProvider)))

      val fileEntryBefore = Await.result(FileEntry.entryFor("project_to_delete.prproj",1), 2 seconds)
      fileEntryBefore must beSuccessfulTry
      fileEntryBefore.get.length mustEqual 1

      val maybeRq = Await.result(ProjectRequest("project_to_delete",1,"Test project entry", 1, "test-user", None, None).hydrate, 10 seconds)
      maybeRq must beSome

      val dateTime = LocalDateTime.now()
      val msg = NewProjectRollback(maybeRq.get)

      val response = Await.result((ac ? msg).mapTo[Either[CreationMessage, CreationMessage]], 10 seconds)
      response must beRight

      val fileEntryAfter = Await.result(FileEntry.entryFor("project_to_delete.prproj",1), 2 seconds)
      fileEntryAfter must beSuccessfulTry
      val entrySeq = fileEntryAfter.get
      entrySeq.length mustEqual 0
    }
  }
}
