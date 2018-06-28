package ProjectCreation

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestProbe
import models.ProjectRequest
import org.specs2.mutable.Specification
import play.api.Configuration
import play.api.db.slick.DatabaseConfigProvider
import play.api.test.WithApplication
import services.actors.creation.GenericCreationActor.{NewProjectRequest, ProjectCreateTransientData, StepSucceded}
import services.actors.creation.{CreationMessage, NotifyPluto}
import slick.jdbc.JdbcProfile
import utils.BuildMyApp

import scala.concurrent.Await
import scala.concurrent.duration._
import akka.pattern.ask
import models.messages.NewProjectCreated

class NotifyPlutoSpec extends Specification with BuildMyApp {
  implicit val timeout:akka.util.Timeout = 30 seconds

  "NotifyPluto->sendRequest" should {
    "retrive the relevant data from database and send a message to MessageProcessor" in new WithApplication(buildApp) {
      private val injector = app.injector

      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val system = injector.instanceOf(classOf[ActorSystem])
      private val config = injector.instanceOf(classOf[Configuration])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val fakeMessageProcessor = TestProbe()

      val ac = system.actorOf(Props(new NotifyPluto(fakeMessageProcessor.ref, dbConfigProvider, config)))
      val maybeRq = Await.result(ProjectRequest("testprojectfile",1,"Test project entry", 1, "test-user", Some(1), Some(1)).hydrate, 10 seconds)
      maybeRq must beSome

      val initialData = ProjectCreateTransientData(None, None, None)
      val msg = NewProjectRequest(maybeRq.get,None,initialData)

      val result = Await.result((ac ? msg).mapTo[CreationMessage],10 seconds)
      result must beAnInstanceOf[StepSucceded]
      fakeMessageProcessor.expectMsgClass(classOf[NewProjectCreated])
    }
  }
}
