package ProjectCreation

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.testkit.TestProbe
import helpers.{JythonOutput, PostrunDataCache}
import models._
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.db.slick.DatabaseConfigProvider
import play.api.test.WithApplication
import services.actors.creation._
import services.actors.creation.GenericCreationActor._
import slick.jdbc.{JdbcBackend, JdbcProfile}
import utils.BuildMyApp

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import models.messages.{NewAdobeUuid, NewAssetFolder}
import play.api.Configuration

class PostrunExecutorSpec extends Specification with BuildMyApp with Mockito {
  implicit val timeout:akka.util.Timeout = 30 seconds

  "PostrunExecutor->NewProjectRequest" should {
    "run a list of postrun actions and send update messages for any asset folder and updated uuid" in new WithApplication(buildApp){
      private val injector = app.injector

      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val system = injector.instanceOf(classOf[ActorSystem])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db
      implicit val config = app.configuration //needed for mocking PostrunAction.run

      val testMessageProcessor = TestProbe()

      val mockedFileEntry = mock[FileEntry]
      mockedFileEntry.getFullPath(any) returns Future("/tmp/someproject.prj")
      mockedFileEntry.hasContent returns true
      mockedFileEntry.id returns Some(1)  //this must be a valid fileID otherwise primary-key association fails

      val mockedProjectEntry = mock[ProjectEntry]
      mockedProjectEntry.getWorkingGroup(any) returns Future(None)
      mockedProjectEntry.getCommission(any) returns Future(None)
      mockedProjectEntry.id returns Some(12345)
      mockedProjectEntry.vidispineProjectId returns None

      val mockedDataCache = mock[PostrunDataCache]
      mockedDataCache.asScala returns Map("created_asset_folder"->"/path/to/my/assetfolder","new_adobe_uuid"->"b8254566-0c69-4000-b990-8082b4b2dd32")

      val mockedPostrunSeq = Seq(
        mock[PostrunAction],
        mock[PostrunAction]
      )

      mockedPostrunSeq.head.run(argThat(===("/tmp/someproject.prj")),argThat(===(mockedProjectEntry)),any,any,argThat(===(None)),argThat(===(None)))(any) returns Future(Success(JythonOutput("Something Worked", "", mockedDataCache, None)))
      mockedPostrunSeq(1).run(argThat(===("/tmp/someproject.prj")),argThat(===(mockedProjectEntry)),any,any,argThat(===(None)),argThat(===(None)))(any) returns Future(Success(JythonOutput("Something else worked", "", mockedDataCache, None)))

      val ac = system.actorOf(Props(new PostrunExecutor(testMessageProcessor.ref, dbConfigProvider, app.configuration)))

      val maybeRq = Await.result(ProjectRequest("testprojectfile",1,"Test project entry", 1, "test-user", Some(1), Some(1), false, false, false).hydrate, 10 seconds)
      maybeRq must beSome

      val initialData = ProjectCreateTransientData(Some(mockedFileEntry), Some(mockedProjectEntry), Some(mockedPostrunSeq))
      val msg = NewProjectRequest(maybeRq.get,None,initialData)
      val result = Await.result((ac ? msg).mapTo[CreationMessage], 10 seconds)

      result must beAnInstanceOf[StepSucceded]
      testMessageProcessor.expectMsg(NewAssetFolder("/path/to/my/assetfolder", mockedProjectEntry.id, mockedProjectEntry.vidispineProjectId))
      testMessageProcessor.expectMsg(NewAdobeUuid(mockedProjectEntry, "b8254566-0c69-4000-b990-8082b4b2dd32"))
    }

    "run a list of postrun actions and not mind if there is no asset folder nor uuid" in new WithApplication(buildApp){
      private val injector = app.injector

      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val system = injector.instanceOf(classOf[ActorSystem])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db
      implicit val config = app.configuration //needed for mocking PostrunAction.run

      val testMessageProcessor = TestProbe()

      val mockedFileEntry = mock[FileEntry]
      mockedFileEntry.getFullPath(any) returns Future("/tmp/someproject.prj")
      mockedFileEntry.hasContent returns true
      mockedFileEntry.id returns Some(1)  //this must be a valid fileID otherwise primary-key association fails

      val mockedProjectEntry = mock[ProjectEntry]
      mockedProjectEntry.getWorkingGroup(any) returns Future(None)
      mockedProjectEntry.getCommission(any) returns Future(None)
      mockedProjectEntry.id returns Some(12345)
      mockedProjectEntry.vidispineProjectId returns None

      val mockedDataCache = mock[PostrunDataCache]
      mockedDataCache.asScala returns Map()

      val mockedPostrunSeq = Seq(
        mock[PostrunAction],
        mock[PostrunAction]
      )

      mockedPostrunSeq.head.run(argThat(===("/tmp/someproject.prj")),argThat(===(mockedProjectEntry)),any,any,argThat(===(None)),argThat(===(None)))(any) returns Future(Success(JythonOutput("Something Worked", "", mockedDataCache, None)))
      mockedPostrunSeq(1).run(argThat(===("/tmp/someproject.prj")),argThat(===(mockedProjectEntry)),any,any,argThat(===(None)),argThat(===(None)))(any) returns Future(Success(JythonOutput("Something else worked", "", mockedDataCache, None)))

      val ac = system.actorOf(Props(new PostrunExecutor(testMessageProcessor.ref, dbConfigProvider, app.configuration)))

      val maybeRq = Await.result(ProjectRequest("testprojectfile",1,"Test project entry", 1, "test-user", Some(1), Some(1), false, false, false).hydrate, 10 seconds)
      maybeRq must beSome

      val initialData = ProjectCreateTransientData(Some(mockedFileEntry), Some(mockedProjectEntry), Some(mockedPostrunSeq))
      val msg = NewProjectRequest(maybeRq.get,None,initialData)
      val result = Await.result((ac ? msg).mapTo[CreationMessage], 10 seconds)

      result must beAnInstanceOf[StepSucceded]
      testMessageProcessor.expectNoMessage(5.seconds)
      testMessageProcessor.expectNoMessage(5.seconds)
    }

    "not fail if the postrun list is empty" in new WithApplication(buildApp){
      private val injector = app.injector

      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val system = injector.instanceOf(classOf[ActorSystem])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db
      implicit val config = app.configuration //needed for mocking PostrunAction.run

      val testMessageProcessor = TestProbe()

      val mockedFileEntry = mock[FileEntry]
      mockedFileEntry.getFullPath(any) returns Future("/tmp/someproject.prj")
      mockedFileEntry.hasContent returns true
      mockedFileEntry.id returns Some(1)  //this must be a valid fileID otherwise primary-key association fails

      val mockedProjectEntry = mock[ProjectEntry]
      mockedProjectEntry.getWorkingGroup(any) returns Future(None)
      mockedProjectEntry.getCommission(any) returns Future(None)
      mockedProjectEntry.id returns Some(12345)
      mockedProjectEntry.vidispineProjectId returns None

      val mockedDataCache = mock[PostrunDataCache]
      mockedDataCache.asScala returns Map()

      val mockedPostrunSeq = Seq()

      val ac = system.actorOf(Props(new PostrunExecutor(testMessageProcessor.ref, dbConfigProvider, app.configuration)))

      val maybeRq = Await.result(ProjectRequest("testprojectfile",1,"Test project entry", 1, "test-user", Some(1), Some(1), false, false, false).hydrate, 10 seconds)
      maybeRq must beSome

      val initialData = ProjectCreateTransientData(Some(mockedFileEntry), Some(mockedProjectEntry), Some(mockedPostrunSeq))
      val msg = NewProjectRequest(maybeRq.get,None,initialData)
      val result = Await.result((ac ? msg).mapTo[CreationMessage], 10 seconds)

      result must beAnInstanceOf[StepSucceded]
      testMessageProcessor.expectNoMessage(5.seconds)
      testMessageProcessor.expectNoMessage(5.seconds)
    }

    "return StepFailed if any postrun fails while executing" in new WithApplication(buildApp){
      private val injector = app.injector

      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val system = injector.instanceOf(classOf[ActorSystem])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db
      implicit val config = app.configuration //needed for mocking PostrunAction.run

      val testMessageProcessor = TestProbe()

      val mockedFileEntry = mock[FileEntry]
      mockedFileEntry.getFullPath(any) returns Future("/tmp/someproject.prj")
      mockedFileEntry.hasContent returns true
      mockedFileEntry.id returns Some(1)  //this must be a valid fileID otherwise primary-key association fails

      val mockedProjectEntry = mock[ProjectEntry]
      mockedProjectEntry.getWorkingGroup(any) returns Future(None)
      mockedProjectEntry.getCommission(any) returns Future(None)
      mockedProjectEntry.id returns Some(12345)
      mockedProjectEntry.vidispineProjectId returns None

      val mockedDataCache = mock[PostrunDataCache]
      mockedDataCache.asScala returns Map()

      val mockedPostrunSeq = Seq(
        mock[PostrunAction],
        mock[PostrunAction]
      )

      mockedPostrunSeq.head.run(argThat(===("/tmp/someproject.prj")),argThat(===(mockedProjectEntry)),any,any,argThat(===(None)),argThat(===(None)))(any) returns Future(Success(JythonOutput("Something failed", "", mockedDataCache, Some(new RuntimeException("leakage!")))))
      mockedPostrunSeq(1).run(argThat(===("/tmp/someproject.prj")),argThat(===(mockedProjectEntry)),any,any,argThat(===(None)),argThat(===(None)))(any) returns Future(Success(JythonOutput("Something else worked", "", mockedDataCache, None)))

      val ac = system.actorOf(Props(new PostrunExecutor(testMessageProcessor.ref, dbConfigProvider, app.configuration)))

      val maybeRq = Await.result(ProjectRequest("testprojectfile",1,"Test project entry", 1, "test-user", Some(1), Some(1), false, false, false).hydrate, 10 seconds)
      maybeRq must beSome

      val initialData = ProjectCreateTransientData(Some(mockedFileEntry), Some(mockedProjectEntry), Some(mockedPostrunSeq))
      val msg = NewProjectRequest(maybeRq.get,None,initialData)
      val result = Await.result((ac ? msg).mapTo[CreationMessage], 10 seconds)

      result must beAnInstanceOf[StepFailed]
      testMessageProcessor.expectNoMessage(5.seconds)
      testMessageProcessor.expectNoMessage(5.seconds)
    }

    "return StepFailed if any postrun fails while starting up" in new WithApplication(buildApp){
      private val injector = app.injector

      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val system = injector.instanceOf(classOf[ActorSystem])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db
      implicit val config = app.configuration //needed for mocking PostrunAction.run

      val testMessageProcessor = TestProbe()

      val mockedFileEntry = mock[FileEntry]
      mockedFileEntry.getFullPath(any) returns Future("/tmp/someproject.prj")
      mockedFileEntry.hasContent returns true
      mockedFileEntry.id returns Some(1)  //this must be a valid fileID otherwise primary-key association fails

      val mockedProjectEntry = mock[ProjectEntry]
      mockedProjectEntry.getWorkingGroup(any) returns Future(None)
      mockedProjectEntry.getCommission(any) returns Future(None)
      mockedProjectEntry.id returns Some(12345)
      mockedProjectEntry.vidispineProjectId returns None

      val mockedDataCache = mock[PostrunDataCache]
      mockedDataCache.asScala returns Map()

      val mockedPostrunSeq = Seq(
        mock[PostrunAction],
        mock[PostrunAction]
      )

      mockedPostrunSeq.head.run(argThat(===("/tmp/someproject.prj")),argThat(===(mockedProjectEntry)),any,any,argThat(===(None)),argThat(===(None)))(any) returns Future(Failure(new RuntimeException("The end is nigh!")))
      mockedPostrunSeq(1).run(argThat(===("/tmp/someproject.prj")),argThat(===(mockedProjectEntry)),any,any,argThat(===(None)),argThat(===(None)))(any) returns Future(Success(JythonOutput("Something else worked", "", mockedDataCache, None)))

      val ac = system.actorOf(Props(new PostrunExecutor(testMessageProcessor.ref, dbConfigProvider, app.configuration)))

      val maybeRq = Await.result(ProjectRequest("testprojectfile",1,"Test project entry", 1, "test-user", Some(1), Some(1), false, false, false).hydrate, 10 seconds)
      maybeRq must beSome

      val initialData = ProjectCreateTransientData(Some(mockedFileEntry), Some(mockedProjectEntry), Some(mockedPostrunSeq))
      val msg = NewProjectRequest(maybeRq.get,None,initialData)
      val result = Await.result((ac ? msg).mapTo[CreationMessage], 10 seconds)

      result must beAnInstanceOf[StepFailed]
      testMessageProcessor.expectNoMessage(5.seconds)
      testMessageProcessor.expectNoMessage(5.seconds)
    }

    "return StepFailed if any setup future fails" in new WithApplication(buildApp){
      private val injector = app.injector

      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val system = injector.instanceOf(classOf[ActorSystem])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db
      implicit val config = app.configuration //needed for mocking PostrunAction.run

      val testMessageProcessor = TestProbe()

      val mockedFileEntry = mock[FileEntry]
      mockedFileEntry.getFullPath(any) answers { db=> throw new RuntimeException("Whoops!")}
      mockedFileEntry.hasContent returns true
      mockedFileEntry.id returns Some(1)  //this must be a valid fileID otherwise primary-key association fails

      val mockedProjectEntry = mock[ProjectEntry]
      mockedProjectEntry.getWorkingGroup(any) returns Future(None)
      mockedProjectEntry.getCommission(any) returns Future(None)
      mockedProjectEntry.id returns Some(12345)
      mockedProjectEntry.vidispineProjectId returns None

      val mockedDataCache = mock[PostrunDataCache]
      mockedDataCache.asScala returns Map()

      val mockedPostrunSeq = Seq(
        mock[PostrunAction],
        mock[PostrunAction]
      )

      mockedPostrunSeq.head.run(argThat(===("/tmp/someproject.prj")),argThat(===(mockedProjectEntry)),any,any,argThat(===(None)),argThat(===(None)))(any) returns Future(Failure(new RuntimeException("The end is nigh!")))
      mockedPostrunSeq(1).run(argThat(===("/tmp/someproject.prj")),argThat(===(mockedProjectEntry)),any,any,argThat(===(None)),argThat(===(None)))(any) returns Future(Success(JythonOutput("Something else worked", "", mockedDataCache, None)))

      val ac = system.actorOf(Props(new PostrunExecutor(testMessageProcessor.ref, dbConfigProvider, app.configuration)))

      val maybeRq = Await.result(ProjectRequest("testprojectfile",1,"Test project entry", 1, "test-user", Some(1), Some(1), false, false, false).hydrate, 10 seconds)
      maybeRq must beSome

      val initialData = ProjectCreateTransientData(Some(mockedFileEntry), Some(mockedProjectEntry), Some(mockedPostrunSeq))
      val msg = NewProjectRequest(maybeRq.get,None,initialData)
      val result = Await.result((ac ? msg).mapTo[CreationMessage], 10 seconds)

      result must beAnInstanceOf[StepFailed]
      testMessageProcessor.expectNoMessage(5.seconds)
      testMessageProcessor.expectNoMessage(5.seconds)
    }
  }

  "PostrunExecutor->NewProjectRollback" should {
    "return StepSucceeded" in new WithApplication(buildApp){
      private val injector = app.injector

      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val system = injector.instanceOf(classOf[ActorSystem])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val testMessageProcessor = TestProbe()

      val ac = system.actorOf(Props(new PostrunExecutor(testMessageProcessor.ref, dbConfigProvider, app.configuration)))

      val maybeRq = Await.result(ProjectRequest("testprojectfile",1,"Test project entry", 1, "test-user", None, None,deletable=true, deep_archive=false, sensitive=false).hydrate, 10 seconds)
      maybeRq must beSome

      val initialData = ProjectCreateTransientData(None, None,None)
      val msg = NewProjectRollback(maybeRq.get,initialData)
      val result = Await.result((ac ? msg).mapTo[CreationMessage], 10 seconds)
      result must beAnInstanceOf[StepSucceded]

    }
  }
}
