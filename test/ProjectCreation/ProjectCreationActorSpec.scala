package ProjectCreation
import akka.actor.{ActorRef, ActorSystem, Props}
import org.specs2.mutable._
import akka.testkit._
import models.{ProjectRequest, ProjectRequestFull}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import slick.jdbc.JdbcProfile
import testHelpers.TestDatabase
import services.actors.ProjectCreationActor
import services.actors.creation.GenericCreationActor.{NewProjectRequest, ProjectCreateSucceeded, StepSucceded}
import akka.pattern.ask
import akka.testkit
import services.actors.creation.CreationMessage

import scala.concurrent.Await
import scala.concurrent.duration._

class ProjectCreationActorSpec extends Specification {
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
}
