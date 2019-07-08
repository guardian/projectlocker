package services

import akka.actor.ActorSystem
import akka.http.scaladsl.HttpExt
import akka.stream.ActorMaterializer
import akka.testkit.TestProbe
import models.PlutoWorkingGroup
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.{Configuration, Logger}
import services.PlutoWGCommissionScanner.RefreshCommissionsForWG
import slick.jdbc.{JdbcBackend, JdbcProfile}
import utils.AkkaTestkitSpecs2Support

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Try

class PlutoWGCommissionScannerFunctionsSpec extends Specification with Mockito {
  "PlutoWGCommissionScannerFunctions.receivedWorkingGroupData" should {
    "ensure that each group is recorded and only return success when they are all done" in new AkkaTestkitSpecs2Support {
      val mockedHttp = mock[HttpExt]
      val testProbe = TestProbe()
      class TestClass extends PlutoWGCommissionScannerFunctions {
        override protected def getHttp: HttpExt = mockedHttp

        override protected val ownRef = testProbe.ref

        override val configuration = Configuration.empty

        override protected val logger = Logger(getClass)

        override implicit val materializer = mock[ActorMaterializer]
        override implicit val actorSystem = mock[ActorSystem]
      }

      val updatedWorkingGroup = PlutoWorkingGroup(Some(123),None,"Updated","same-uuid")
      val mockedEnsureRecorded = mock[Function1[JdbcBackend#DatabaseDef, Future[PlutoWorkingGroup]]]
      mockedEnsureRecorded.apply(any) returns Future(updatedWorkingGroup)

      val testData = List(
        new PlutoWorkingGroup(Some(1),None,"something","some-uuid") {
          override def ensureRecorded(implicit db: JdbcBackend#DatabaseDef) = mockedEnsureRecorded(db)
        },
        new PlutoWorkingGroup(Some(2),None,"something else","some-other-uuid") {
          override def ensureRecorded(implicit db: JdbcBackend#DatabaseDef) = mockedEnsureRecorded(db)
        }
      )

      implicit val mockedDb = mock[JdbcProfile#Backend#Database]

      val toTest = new TestClass
      val result = Await.result(toTest.receivedWorkingGroupData(testData), 30 seconds)

      there were two(mockedEnsureRecorded).apply(any)

      result.length mustEqual 2
      //we force this by mocking ensureRecorded, above.
      //In practise, this test ensures that the working group record sent on in the chain is the one that was
      //returned from ensureRecorded (i.e., has an updated ID if it had not been saved before)
      testProbe.expectMsg(RefreshCommissionsForWG(updatedWorkingGroup))
      testProbe.expectMsg(RefreshCommissionsForWG(updatedWorkingGroup))
      testProbe.expectNoMessage(2 seconds)
    }

    "return an error if any save operation fails" in new AkkaTestkitSpecs2Support {
      val mockedHttp = mock[HttpExt]
      val testProbe = TestProbe()
      class TestClass extends PlutoWGCommissionScannerFunctions {
        override protected def getHttp: HttpExt = mockedHttp

        override protected val ownRef = testProbe.ref

        override val configuration = Configuration.empty

        override protected val logger = Logger(getClass)

        override implicit val materializer = mock[ActorMaterializer]
        override implicit val actorSystem = mock[ActorSystem]
      }

      val updatedWorkingGroup = PlutoWorkingGroup(Some(123),None,"Updated","same-uuid")
      val mockedEnsureRecorded = mock[Function1[JdbcBackend#DatabaseDef, Future[PlutoWorkingGroup]]]
      mockedEnsureRecorded.apply(any) returns Future(updatedWorkingGroup)

      val mockedEnsureRecordedFailure = mock[Function1[JdbcBackend#DatabaseDef, Future[PlutoWorkingGroup]]]
      mockedEnsureRecordedFailure.apply(any) returns Future.failed(new RuntimeException("kaboom"))

      val testData = List(
        new PlutoWorkingGroup(Some(1),None,"something","some-uuid") {
          override def ensureRecorded(implicit db: JdbcBackend#DatabaseDef) = mockedEnsureRecorded(db)
        },
        new PlutoWorkingGroup(Some(2),None,"something else","some-other-uuid") {
          override def ensureRecorded(implicit db: JdbcBackend#DatabaseDef) = mockedEnsureRecordedFailure(db)
        }
      )

      implicit val mockedDb = mock[JdbcProfile#Backend#Database]

      val toTest = new TestClass

      val result = Try { Await.result(toTest.receivedWorkingGroupData(testData), 30 seconds) }

      there was one(mockedEnsureRecorded).apply(any)

      result must beFailedTry
      //we force this by mocking ensureRecorded, above.
      //In practise, this test ensures that the working group record sent on in the chain is the one that was
      //returned from ensureRecorded (i.e., has an updated ID if it had not been saved before)
      testProbe.expectMsg(RefreshCommissionsForWG(updatedWorkingGroup))
      testProbe.expectNoMessage(2 seconds)
    }
  }
}
