package ProjectCreation

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.testkit.TestProbe
import models.{FileEntry, ProjectEntry, ProjectRequest, ProjectRequestFull}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.db.slick.DatabaseConfigProvider
import play.api.test.WithApplication
import services.actors.creation.{CopySourceFile, CreateFileEntry, CreateProjectEntry, CreationMessage}
import services.actors.creation.GenericCreationActor._
import slick.jdbc.{JdbcBackend, JdbcProfile}
import utils.BuildMyApp

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import models.messages.NewProjectCreated

class CreateProjectEntrySpec extends Specification with BuildMyApp with Mockito {
  implicit val timeout:akka.util.Timeout = 30 seconds

  "CreateProjectEntry->NewProjectRequest" should {
    "create a new entry in the database and call out to send a created message, if there is a commission" in new WithApplication(buildApp){
      private val injector = app.injector

      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val system = injector.instanceOf(classOf[ActorSystem])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val testMessageProcessor = TestProbe()

      val mockedFileEntry = mock[FileEntry]
      mockedFileEntry.hasContent returns true
      mockedFileEntry.id returns Some(1)  //this must be a valid fileID otherwise primary-key association fails

      val ac = system.actorOf(Props(new CreateProjectEntry(testMessageProcessor.ref, dbConfigProvider)))

      val maybeRq = Await.result(ProjectRequest("testprojectfile",1,"Test project entry", 1, "test-user", Some(1), Some(1), false, false, false).hydrate, 10 seconds)
      maybeRq must beSome

      val initialData = ProjectCreateTransientData(Some(mockedFileEntry), None, None)
      val msg = NewProjectRequest(maybeRq.get,None,initialData)
      val result = Await.result((ac ? msg).mapTo[CreationMessage], 10 seconds)
      result must beAnInstanceOf[StepSucceded]
      result.asInstanceOf[StepSucceded].updatedData.createdProjectEntry must beSome
      testMessageProcessor.expectMsgClass(classOf[NewProjectCreated])
    }

    "create a new entry in the database and not call out to send a created message, if there is no commission" in new WithApplication(buildApp){
      private val injector = app.injector

      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val system = injector.instanceOf(classOf[ActorSystem])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val testMessageProcessor = TestProbe()

      val mockedFileEntry = mock[FileEntry]
      mockedFileEntry.hasContent returns true
      mockedFileEntry.id returns Some(1)  //this must be a valid fileID otherwise primary-key association fails

      val ac = system.actorOf(Props(new CreateProjectEntry(testMessageProcessor.ref, dbConfigProvider)))

      val maybeRq = Await.result(ProjectRequest("testprojectfile",1,"Test project entry", 1, "test-user", None, None, false, false, false).hydrate, 10 seconds)
      maybeRq must beSome

      val initialData = ProjectCreateTransientData(Some(mockedFileEntry), None, None)
      val msg = NewProjectRequest(maybeRq.get,None,initialData)
      val result = Await.result((ac ? msg).mapTo[CreationMessage], 10 seconds)
      result must beAnInstanceOf[StepSucceded]
      result.asInstanceOf[StepSucceded].updatedData.createdProjectEntry must beSome
      testMessageProcessor.expectNoMessage(5 seconds)
    }

    "fail if the provided file has no content attached" in new WithApplication(buildApp){
      private val injector = app.injector

      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val system = injector.instanceOf(classOf[ActorSystem])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val testMessageProcessor = TestProbe()

      val mockedFileEntry = mock[FileEntry]
      mockedFileEntry.hasContent returns false
      mockedFileEntry.id returns Some(1)  //this must be a valid fileID otherwise primary-key association fails

      val ac = system.actorOf(Props(new CreateProjectEntry(testMessageProcessor.ref, dbConfigProvider)))

      val maybeRq = Await.result(ProjectRequest("testprojectfile",1,"Test project entry", 1, "test-user", None, None, false, false, false).hydrate, 10 seconds)
      maybeRq must beSome

      val initialData = ProjectCreateTransientData(Some(mockedFileEntry), None, None)
      val msg = NewProjectRequest(maybeRq.get,None,initialData)
      val result = Await.result((ac ? msg).mapTo[CreationMessage], 10 seconds)
      result must beAnInstanceOf[StepFailed]
    }

    "fail if a database error occurs" in new WithApplication(buildApp){
      private val injector = app.injector

      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val system = injector.instanceOf(classOf[ActorSystem])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val testMessageProcessor = TestProbe()

      val mockedFileEntry = mock[FileEntry]
      mockedFileEntry.hasContent returns false
      mockedFileEntry.id returns Some(548)  //deliberately not a valid fileID

      val ac = system.actorOf(Props(new CreateProjectEntry(testMessageProcessor.ref, dbConfigProvider)))

      val maybeRq = Await.result(ProjectRequest("testprojectfile",1,"Test project entry", 1, "test-user", None, None, false, false, false).hydrate, 10 seconds)
      maybeRq must beSome

      val initialData = ProjectCreateTransientData(Some(mockedFileEntry), None, None)
      val msg = NewProjectRequest(maybeRq.get,None,initialData)
      val result = Await.result((ac ? msg).mapTo[CreationMessage], 10 seconds)
      result must beAnInstanceOf[StepFailed]
    }

  }

  "CreateProjectEntry->NewProjectRollback" should {
    "remove the provided project entry from the database" in new WithApplication(buildApp){
      private val injector = app.injector

      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val system = injector.instanceOf(classOf[ActorSystem])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val testMessageProcessor = TestProbe()

      val mockedFileEntry = mock[FileEntry]
      mockedFileEntry.hasContent returns true
      mockedFileEntry.id returns Some(1)  //this must be a valid fileID otherwise primary-key association fails

      val mockedProjectEntry = mock[ProjectEntry]
      mockedProjectEntry.removeFromDatabase(any) returns Future(Success(1))

      val ac = system.actorOf(Props(new CreateProjectEntry(testMessageProcessor.ref, dbConfigProvider)))

      val maybeRq = Await.result(ProjectRequest("testprojectfile",1,"Test project entry", 1, "test-user", Some(1), Some(1), false, false, false).hydrate, 10 seconds)
      maybeRq must beSome

      val initialData = ProjectCreateTransientData(Some(mockedFileEntry), Some(mockedProjectEntry), None)
      val msg = NewProjectRollback(maybeRq.get,initialData)
      val result = Await.result((ac ? msg).mapTo[CreationMessage], 10 seconds)
      result must beAnInstanceOf[StepSucceded]
      result.asInstanceOf[StepSucceded].updatedData.createdProjectEntry must beNone
      there was one(mockedProjectEntry).removeFromDatabase(any)
    }
  }

}
