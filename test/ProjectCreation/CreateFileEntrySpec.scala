import java.sql.Timestamp
import java.time.LocalDateTime

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import models.{FileEntry, ProjectRequest, ProjectRequestFull}
import org.specs2.mutable.Specification
import play.api.db.slick.DatabaseConfigProvider
import play.api.test.WithApplication
import services.actors.creation.{CreateFileEntry, CreationMessage}
import services.actors.creation.GenericCreationActor._
import slick.jdbc.{JdbcBackend, JdbcProfile}
import utils.BuildMyApp

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Try}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class CreateFileEntrySpec extends Specification with BuildMyApp {
  implicit val timeout:akka.util.Timeout = 30.seconds

  "CreateFileEntry->NewProjectRequest" should {
    "create a file entry and respond with StepSucceeded" in new WithApplication(buildApp){
      private val injector = app.injector

      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val system = injector.instanceOf(classOf[ActorSystem])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val ac = system.actorOf(Props(new CreateFileEntry(dbConfigProvider)))

      val initialData = ProjectCreateTransientData(None, None, None)

      val fileEntryBefore = Await.result(FileEntry.entryFor("testfile.prproj",1), 2 seconds)
      fileEntryBefore must beSuccessfulTry
      fileEntryBefore.get.length mustEqual 0

      val maybeRq = Await.result(ProjectRequest("testfile",1,"Test project entry", 1, "test-user", None, None, false, false, false).hydrate, 10 seconds)
      maybeRq must beSome

      val dateTime = LocalDateTime.now()
      val msg = NewProjectRequest(maybeRq.get, Some(dateTime), initialData)

      val response = Await.result((ac ? msg).mapTo[CreationMessage], 10 seconds)
      response must beAnInstanceOf[StepSucceded]

      val fileEntryAfter = Await.result(FileEntry.entryFor("testfile.prproj",1), 2 seconds)
      fileEntryAfter must beSuccessfulTry
      val entrySeq = fileEntryAfter.get
      entrySeq.length mustEqual 1
      entrySeq.head.hasContent must beFalse
    }

    "return StepFailed with an exception if there is an error" in new WithApplication(buildApp){
      private val injector = app.injector

      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val system = injector.instanceOf(classOf[ActorSystem])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val ex=new RuntimeException("My hovercraft is full of eels")
      val ac = system.actorOf(Props(new CreateFileEntry(dbConfigProvider) {
        override def getDestFileFor(rq: ProjectRequestFull, recordTimestamp: Timestamp)
                                   (implicit db: JdbcBackend#DatabaseDef): Future[FileEntry] = Future.failed(ex)
      }))

      val initialData = ProjectCreateTransientData(None, None, None)

      val fileEntryBefore = Await.result(FileEntry.entryFor("testfile2.prproj",1), 2 seconds)
      fileEntryBefore must beSuccessfulTry
      fileEntryBefore.get.length mustEqual 0

      val maybeRq = Await.result(ProjectRequest("testfile2",1,"Test project entry", 1, "test-user", None, None,false, false, false).hydrate, 10 seconds)
      maybeRq must beSome

      val dateTime = LocalDateTime.now()
      val msg = NewProjectRequest(maybeRq.get, Some(dateTime), initialData)

      val response = Await.result((ac ? msg).mapTo[CreationMessage], 10 seconds)
      response mustEqual StepFailed(initialData, ex)

      val fileEntryAfter = Await.result(FileEntry.entryFor("testfile2.prproj",1), 2 seconds)
      fileEntryAfter must beSuccessfulTry
      val entrySeq = fileEntryAfter.get
      entrySeq.length mustEqual 0
    }
  }

  "CreateFileEntry->NewProjectRollback" should {
    "delete an existing file entry" in new WithApplication(buildApp){
      private val injector = app.injector

      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val system = injector.instanceOf(classOf[ActorSystem])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val ac = system.actorOf(Props(new CreateFileEntry(dbConfigProvider)))

      val fileEntryBefore = Await.result(FileEntry.entryFor("project_to_delete.prproj",1), 2 seconds)
      fileEntryBefore must beSuccessfulTry
      fileEntryBefore.get.length mustEqual 1

      val initialData = ProjectCreateTransientData(None, None, None)

      val maybeRq = Await.result(ProjectRequest("project_to_delete",1,"Test project entry", 1, "test-user", None, None, false, false, false).hydrate, 10 seconds)
      maybeRq must beSome

      val dateTime = LocalDateTime.now()
      val msg = NewProjectRollback(maybeRq.get, initialData)

      val response = Await.result((ac ? msg).mapTo[CreationMessage], 10 seconds)
      response must beAnInstanceOf[StepSucceded]

      val fileEntryAfter = Await.result(FileEntry.entryFor("project_to_delete.prproj",1), 2 seconds)
      fileEntryAfter must beSuccessfulTry
      val entrySeq = fileEntryAfter.get
      entrySeq.length mustEqual 0
    }
  }
}