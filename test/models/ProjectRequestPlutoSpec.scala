package models

import org.specs2.mutable.Specification
import play.api.db.slick.DatabaseConfigProvider
import play.api.test.WithApplication
import slick.jdbc.PostgresProfile
import utils.BuildMyApp

import scala.concurrent.Await
import scala.concurrent.duration._

class ProjectRequestPlutoSpec extends Specification with BuildMyApp {
  "ProjectRequestPluto.getDefaultProjectTemplate" should {
    "return the project matching a subtype if present" in new WithApplication(buildApp){
      private val injector = app.injector

      protected val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      protected implicit val db = dbConfigProvider.get[PostgresProfile].db

      val rq = ProjectRequestPluto("testfilename","test title",
        "330b4d84-ef24-41e2-b093-0d15829afa64",Some("0145d384-f9ae-42af-9b2e-48adca11a7e4"),
        "username","14005e61-4115-4e4b-a252-247264e914d8","VX-123","VX-456")

      val result = Await.result(rq.getDefaultProjectTemplate, 10 seconds)
      result must beRight
      result.right.get.id must beSome(3)
    }

    "return the project matching main type if subtype does not exist" in new WithApplication(buildApp){
      private val injector = app.injector

      protected val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      protected implicit val db = dbConfigProvider.get[PostgresProfile].db

      val rq = ProjectRequestPluto("testfilename","test title",
        "330b4d84-ef24-41e2-b093-0d15829afa64",Some("ae30d9c8-d954-4c8b-846e-0ca28daa21a8"),
        "username","14005e61-4115-4e4b-a252-247264e914d8","VX-123","VX-456")

      val result = Await.result(rq.getDefaultProjectTemplate, 10 seconds)
      result must beRight
      result.right.get.id must beSome(1)
    }

    "return the project matching main type if subtype is not present" in new WithApplication(buildApp){
      private val injector = app.injector

      protected val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      protected implicit val db = dbConfigProvider.get[PostgresProfile].db

      val rq = ProjectRequestPluto("testfilename","test title",
        "330b4d84-ef24-41e2-b093-0d15829afa64",None,
        "username","14005e61-4115-4e4b-a252-247264e914d8","VX-123","VX-456")

      val result = Await.result(rq.getDefaultProjectTemplate, 10 seconds)
      result must beRight
      result.right.get.id must beSome(1)
    }
  }
}
