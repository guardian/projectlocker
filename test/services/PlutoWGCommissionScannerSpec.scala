package services

import java.sql.Timestamp
import java.time.Instant

import akka.actor.Props
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.{HttpEntity, HttpResponse}
import akka.testkit.TestProbe
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.Configuration
import play.api.db.slick.DatabaseConfigProvider
import utils.AkkaTestkitSpecs2Support
import akka.pattern.ask
import models.{PlutoCommission, PlutoCommissionRow, PlutoWorkingGroup}
import slick.basic.DatabaseConfig
import slick.jdbc.{JdbcProfile, PostgresProfile}
import slick.lifted.Tag

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

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
      val config = Configuration.from(Map(
        "pluto.sync_enabled"->"yes",
        "pluto.server_url"->"https://my-server",
        "pluto.username"->"fred",
        "pluto.password"->"rubbish"
      ))
      val returnContent =
        """[{
          |"id": 1,
          |"name": "something",
          |"uuid": "7aae3d33-c0ae-40ff-88f7-2f61234066f0"
          |},
          |{
          |"id": 2,
          |"name": "something else",
          |"uuid": "36EBF07E-0D4E-4B62-A61A-6610BB62D121"
          |}]""".stripMargin

      val testProbe = TestProbe()
      val mockedReceivedWorkingGroupData = mock[List[PlutoWorkingGroup]=>Future[List[PlutoWorkingGroup]]]
      mockedReceivedWorkingGroupData.apply(any) returns Future(List())

      val mockedResponse = HttpResponse(entity = HttpEntity(returnContent))
      val mockedHttp:HttpExt = mock[HttpExt]

      mockedHttp.singleRequest(any,any,any,any) returns Future(mockedResponse)
      val toTestActor = system.actorOf(Props(new PlutoWGCommissionScanner(config, system, getMockedDbConfigProvider) {
        override protected val ownRef = testProbe.ref

        override protected def getHttp: HttpExt = mockedHttp

        override def receivedWorkingGroupData(wgList:List[PlutoWorkingGroup])(implicit ec:ExecutionContext, db:JdbcProfile#Backend#Database): Future[List[PlutoWorkingGroup]] = mockedReceivedWorkingGroupData(wgList)
      }))

      val result = Await.result(toTestActor ? RefreshWorkingGroups, 30 seconds)
      result mustEqual akka.actor.Status.Success
      there was one(mockedReceivedWorkingGroupData).apply(List(
        PlutoWorkingGroup(Some(1),None,"something","7aae3d33-c0ae-40ff-88f7-2f61234066f0"),
        PlutoWorkingGroup(Some(2),None,"something else","36EBF07E-0D4E-4B62-A61A-6610BB62D121")))
    }
  }

  "PlutoWGCommissionScanner ! RefreshCommissionsForWG" should {
    "fail if no ID is present in the passed working group" in new AkkaTestkitSpecs2Support {
      val testData = PlutoWorkingGroup(None,None,"something","something")
      val config = Configuration.empty
      val dbConfigProvider = getMockedDbConfigProvider
      val testProbe = TestProbe()
      val mockedHttp:HttpExt = mock[HttpExt]

      val toTestActor = system.actorOf(Props(new PlutoWGCommissionScanner(config, system, dbConfigProvider){
        override protected val ownRef = testProbe.ref

        override protected def getHttp: HttpExt = mockedHttp
      }))

      val result = Try { Await.result((toTestActor ? RefreshCommissionsForWG(testData)).mapTo[akka.actor.Status.Status], 30 seconds) }
      result must beFailedTry
    }

    /* I can't get the mocking to work for db.run() so can't use this test at present.
    "pass RefreshCommissionsInfo with the most recent working group" in new AkkaTestkitSpecs2Support {
      val testData = PlutoWorkingGroup(Some(2),None,"something","something")
      val testComm = PlutoCommission(Some(2),1,"AA",Timestamp.from(Instant.now()), Timestamp.from(Instant.now()),"some-commission","In Production",None, 1)
      val config = Configuration.empty
      val dbConfigProvider = mock[DatabaseConfigProvider]
      //val mockedDbConfig = mock[DatabaseConfig[PostgresProfile]]
      val mockedDb = mock[PostgresProfile#Backend#Database]

      dbConfigProvider.get[PostgresProfile] returns new DatabaseConfig[PostgresProfile] {
        override def profileName: String = "testprofile"

        override def profileIsObject: Boolean = false

        override def config: Config = mock[Config]

        override def db = mockedDb

        override val profile = PostgresProfile

        override val driver = PostgresProfile
      }

      mockedDb.run[Try[Seq[PlutoCommissionRow#TableElementType]]](any) returns Future(Success(Seq(testComm)))
      mockedDb.run[Try[Seq[PlutoCommission]]](any) returns Future(Success(Seq(testComm)))
      //mockedDb.run[AnyRef](any) returns Future(Success(Seq(testComm)))

      val testProbe = TestProbe()
      val mockedHttp:HttpExt = mock[HttpExt]

      val toTestActor = system.actorOf(Props(new PlutoWGCommissionScanner(config, system, dbConfigProvider){
        override protected val ownRef = testProbe.ref

        override protected def getHttp: HttpExt = mockedHttp
      }))

      toTestActor ! RefreshCommissionsForWG(testData)

      testProbe.expectMsg(3 seconds, RefreshCommissionsInfo(testData,"AA","",0,10))
    }
    */
  }
}
