import java.util.UUID

import models.{PlutoWorkingGroup, PlutoWorkingGroupSerializer}
import org.specs2.mutable.Specification
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import slick.jdbc.{JdbcProfile, PostgresProfile}
import testHelpers.TestDatabase

import scala.concurrent.Await
import scala.concurrent.duration._

class PlutoWorkingGroupSpec extends Specification with PlutoWorkingGroupSerializer {
  protected val application = new GuiceApplicationBuilder()
    .overrides(bind[DatabaseConfigProvider].to[TestDatabase.testDbProvider])
    .build

  private val injector = application.injector

  protected val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
  protected implicit val db = dbConfigProvider.get[PostgresProfile].db

  "PlutoWorkingGroup" should {
    "automatically deserialize working group entries" in {
      val jsonData = """{"name":"test working group","uuid":"625CA9CB-F8F6-49E2-ABFD-3A1BC3D2E371"}"""
      val content = Json.parse(jsonData).as[PlutoWorkingGroup]

      content.name mustEqual "test working group"
      content.uuid mustEqual  "625CA9CB-F8F6-49E2-ABFD-3A1BC3D2E371"
      content.id must beNone
      content.hide must beNone
    }

    "save deserialized working group entries to the database" in {
      val jsonData = """{"name":"test working group","uuid":"625CA9CB-F8F6-49E2-ABFD-3A1BC3D2E371"}"""
      val content = Json.parse(jsonData).as[PlutoWorkingGroup]

      content.name mustEqual "test working group"
      content.uuid mustEqual "625CA9CB-F8F6-49E2-ABFD-3A1BC3D2E371"
      content.id must beNone
      content.hide must beNone

      val result = Await.result(content.save, 10.seconds)
      result must beSuccessfulTry
      result.get.id must beSome
    }

    "not re-save a model that already exists" in {
      val jsonData = """{"name":"test working group","uuid":"DA60602E-55C1-4F2A-8EDD-2737BEB4916E"}"""
      val content = Json.parse(jsonData).as[PlutoWorkingGroup]

      content.name mustEqual "test working group"
      content.uuid mustEqual "DA60602E-55C1-4F2A-8EDD-2737BEB4916E"
      content.id must beNone
      content.hide must beNone

      val result = Await.result(content.ensureRecorded, 10.seconds)
      result must beSuccessfulTry
      result.get.id must beSome
    }
  }


}
