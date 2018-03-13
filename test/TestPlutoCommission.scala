import helpers.ProjectCreateHelper
import models._
import org.specs2.mutable.Specification
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import play.api.libs.functional.syntax._
import slick.jdbc.JdbcProfile
import testHelpers.TestDatabase

import scala.concurrent.Await
import scala.concurrent.duration._

class TestPlutoCommission extends Specification with TimestampSerialization {
  protected val application = new GuiceApplicationBuilder()
    .overrides(bind[DatabaseConfigProvider].to[TestDatabase.testDbProvider])
    .build

  private val injector = application.injector

  protected val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
  protected implicit val db = dbConfigProvider.get[JdbcProfile].db

  "PlutoCommission" should {
    "deserialize a response from the pluto server " in {
      val jsonData = Json.parse("""[
                                  |    {
                                  |        "collection_id": 11,
                                  |        "user": 1,
                                  |        "created": "2017-12-04T16:11:23.632",
                                  |        "updated": "2017-12-04T16:11:28.288",
                                  |        "gnm_commission_title": "addasads",
                                  |        "gnm_commission_status": "New",
                                  |        "gnm_commission_workinggroup": "8b2bc331-7a11-40d0-a1e5-1266bdf8dce5",
                                  |        "gnm_commission_description": null,
                                  |        "gnm_commission_owner": [
                                  |            1
                                  |        ]
                                  |    },
                                  |    {
                                  |        "collection_id": 26,
                                  |        "user": 1,
                                  |        "created": "2017-12-06T15:17:19.425",
                                  |        "updated": "2017-12-06T15:17:20.808",
                                  |        "gnm_commission_title": "fwqggrgqggreqgr",
                                  |        "gnm_commission_status": "New",
                                  |        "gnm_commission_workinggroup": "8b2bc331-7a11-40d0-a1e5-1266bdf8dce5",
                                  |        "gnm_commission_description": null,
                                  |        "gnm_commission_owner": [
                                  |            1
                                  |        ]
                                  |    },
                                  |    {
                                  |        "collection_id": 13,
                                  |        "user": 1,
                                  |        "created": "2017-12-04T16:18:12.105",
                                  |        "updated": "2018-01-04T14:14:35.346",
                                  |        "gnm_commission_title": "addasadsf",
                                  |        "gnm_commission_status": "In production",
                                  |        "gnm_commission_workinggroup": "8b2bc331-7a11-40d0-a1e5-1266bdf8dce5",
                                  |        "gnm_commission_description": null,
                                  |        "gnm_commission_owner": [
                                  |            1
                                  |        ]
                                  |    }
                                  |]""".stripMargin)

      val data = jsonData.as[List[JsValue]].map(PlutoCommission.fromServerRepresentation(_,99,"AG"))
      data.length mustEqual 3

      data.head must beSuccessfulTry
      data(1) must beSuccessfulTry
      data(2) must beSuccessfulTry
      println(data(2).get)
      data(2).get.collectionId mustEqual 13
      data(2).get.title mustEqual "addasadsf"
      data(2).get.status mustEqual "In production"

    }
  }

  "PlutoCommission.mostRecentByWorkingGroup" should {
    "return the most recently modified commission in the provided group" in {
      val result = Await.result(PlutoCommission.mostRecentByWorkingGroup(1),10.seconds)

      result must beSuccessfulTry
      result.get must beSome
      result.get.get.collectionId mustEqual 4567
      result.get.get.title mustEqual "My test commission 4"
    }
  }
}
