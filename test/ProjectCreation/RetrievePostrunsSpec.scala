package ProjectCreation
import java.io.{File, FileInputStream}
import java.sql.Timestamp
import java.time.LocalDateTime

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import models._
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.db.slick.DatabaseConfigProvider
import play.api.test.WithApplication
import services.actors.creation.{CreationMessage, RetrievePostruns}
import services.actors.creation.GenericCreationActor._
import slick.jdbc.{JdbcBackend, JdbcProfile}
import utils.BuildMyApp

import scala.concurrent.{Await, Future}
import scala.sys.process._
import scala.util.{Failure, Try}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import org.mockito.Mockito._
import helpers.StorageHelper

class RetrievePostrunsSpec extends Specification with BuildMyApp with Mockito {
  sequential
  implicit val timeout:akka.util.Timeout = 30.seconds

  "RetrievePostruns->NewProjectRequest" should {
    "look up the postruns associated with the provided project type and sort them" in new WithApplication(buildApp){
      private val injector = app.injector

      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val system = injector.instanceOf(classOf[ActorSystem])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val ac = system.actorOf(Props(new RetrievePostruns(dbConfigProvider)))

      val maybeRq = Await.result(ProjectRequest("testprojectfile",1,"Test project entry", 1, "test-user", None, None).hydrate, 10 seconds)
      maybeRq must beSome

      val initialData = ProjectCreateTransientData(None, None,None)
      val msg = NewProjectRequest(maybeRq.get,None,initialData)
      val result = Await.result((ac ? msg).mapTo[CreationMessage], 10 seconds)
      result must beAnInstanceOf[StepSucceded]

      val maybePostrunSeq = result.asInstanceOf[StepSucceded].updatedData.postrunSequence
      println(maybePostrunSeq)
      maybePostrunSeq must beSome
      val postrunSeq = maybePostrunSeq.get

      postrunSeq.length mustEqual 3
      postrunSeq.head.id must beSome(5)
      postrunSeq(1).id must beSome(1)
      postrunSeq(2).id must beSome(2)
    }

    "return an empty list of postruns if none are present" in new WithApplication(buildApp){
      private val injector = app.injector

      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val system = injector.instanceOf(classOf[ActorSystem])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val ac = system.actorOf(Props(new RetrievePostruns(dbConfigProvider)))

      val maybeRq = Await.result(ProjectRequest("testprojectfile",1,"Test project entry", 2, "test-user", None, None).hydrate, 10 seconds)
      maybeRq must beSome

      val initialData = ProjectCreateTransientData(None, None,None)
      val msg = NewProjectRequest(maybeRq.get,None,initialData)
      val result = Await.result((ac ? msg).mapTo[CreationMessage], 10 seconds)
      result must beAnInstanceOf[StepSucceded]

      println(result.asInstanceOf[StepSucceded])
      val maybePostrunSeq = result.asInstanceOf[StepSucceded].updatedData.postrunSequence
      maybePostrunSeq must beSome
      val postrunSeq = maybePostrunSeq.get

      postrunSeq.length mustEqual 0
    }

    "return StepFailed if a database error occurs" in new WithApplication(buildApp){
      private val injector = app.injector

      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val system = injector.instanceOf(classOf[ActorSystem])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val ac = system.actorOf(Props(new RetrievePostruns(dbConfigProvider)))

      val maybeRq = Await.result(ProjectRequest("testprojectfile",1,"Test project entry", 2, "test-user", None, None).hydrate, 10 seconds)
      maybeRq must beSome

      val mockedProjectType = mock[ProjectType]
      val fakeError = new RuntimeException("Drop that explosion!")
      mockedProjectType.postrunActions returns Future(Failure(fakeError))
      val mockedProjectTemplate = mock[ProjectTemplate]
      mockedProjectTemplate.projectType returns Future(mockedProjectType)

      val mockedRq = maybeRq.get.copy(projectTemplate=mockedProjectTemplate)
      val initialData = ProjectCreateTransientData(None, None,None)
      val msg = NewProjectRequest(mockedRq,None,initialData)
      val result = Await.result((ac ? msg).mapTo[CreationMessage], 10 seconds)
      result must beAnInstanceOf[StepFailed]

      result.asInstanceOf[StepFailed].updatedData mustEqual initialData
      result.asInstanceOf[StepFailed].err mustEqual fakeError
    }
  }

  "RetrievePostruns->NewProjectRollback" should {
    "return StepSucceeded" in new WithApplication(buildApp){
      private val injector = app.injector

      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val system = injector.instanceOf(classOf[ActorSystem])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val ac = system.actorOf(Props(new RetrievePostruns(dbConfigProvider)))

      val maybeRq = Await.result(ProjectRequest("testprojectfile",1,"Test project entry", 1, "test-user", None, None).hydrate, 10 seconds)
      maybeRq must beSome

      val initialData = ProjectCreateTransientData(None, None,None)
      val msg = NewProjectRollback(maybeRq.get,initialData)
      val result = Await.result((ac ? msg).mapTo[CreationMessage], 10 seconds)
      result must beAnInstanceOf[StepSucceded]

    }
  }
}
