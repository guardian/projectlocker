package models

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.WithApplication
import slick.jdbc.{JdbcProfile, PostgresProfile}
import testHelpers.TestDatabase
import utils.BuildMyApp

import scala.concurrent.Await
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class PostrunDependencySpec extends Specification with BuildMyApp {
  sequential


  "PostrunDependencyGraph.loadAll" should {
    "load in the dependencies table as a map of data" in new WithApplication(buildApp){
      private val injector = app.injector

      protected val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      protected implicit val db = dbConfigProvider.get[PostgresProfile].db

      val result = Await.result(PostrunDependencyGraph.loadAllById,30.seconds)

      result mustEqual Map(
        1->Seq(5,6),
        4->Seq(5),
        2->Seq(1)
      )
    }
  }
}
