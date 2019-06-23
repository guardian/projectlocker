package services

import akka.actor.Props
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.{HttpEntity, HttpResponse}
import akka.testkit.TestProbe
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.Configuration
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.{JsValue, Json}
import utils.AkkaTestkitSpecs2Support
import akka.pattern.ask
import models.PlutoWorkingGroup
import slick.basic.DatabaseConfig
import slick.jdbc.PostgresProfile

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

class PlutoWGCommissionScannerSpec extends Specification with Mockito {
  import PlutoWGCommissionScanner._
  implicit val timeout:akka.util.Timeout = 30 seconds

  def getMockedDbConfigProvider:DatabaseConfigProvider = {
    val m = mock[DatabaseConfigProvider]
    m.get[PostgresProfile] returns mock[DatabaseConfig[PostgresProfile]]
    m
  }

  "PlutoWGCommissionScanner ! RefreshWorkingGroups" should {
    "perform a GET request to determine working groups and call receivedWorkingGroupData to process the response" in new AkkaTestkitSpecs2Support {
      implicit val ec:ExecutionContext = system.dispatcher
      val config = Configuration.from(Map(
        "pluto.sync_enabled"->"yes",
        "pluto.server_url"->"https://my-server",
        "pluto.username"->"fred",
        "pluto.password"->"rubbish"
      ))
      val returnContent = """{}"""

      val testProbe = TestProbe()
      val mockedReceivedWorkingGroupData = mock[JsValue=>Future[List[PlutoWorkingGroup]]]
      mockedReceivedWorkingGroupData.apply(any) returns Future(List())

      val mockedResponse = HttpResponse(entity = HttpEntity(returnContent))
      val mockedHttp:HttpExt = mock[HttpExt]

      mockedHttp.singleRequest(any,any,any,any) returns Future(mockedResponse)
      val toTestActor = system.actorOf(Props(new PlutoWGCommissionScanner(config, system, getMockedDbConfigProvider) {
        override protected val ownRef = testProbe.ref

        override protected def getHttp: HttpExt = mockedHttp

        override def receivedWorkingGroupData(parsedData: JsValue): Future[List[PlutoWorkingGroup]] = mockedReceivedWorkingGroupData(parsedData)
      }))

      val result = Await.result(toTestActor ? RefreshWorkingGroups, 30 seconds)
      result mustEqual akka.actor.Status.Success
      there was one(mockedReceivedWorkingGroupData).apply(Json.parse(returnContent))
    }
  }
}
