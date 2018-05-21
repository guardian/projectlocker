package models

import org.junit.runner._
import org.specs2.mutable.Specification
import org.specs2.runner._
import play.api.db.slick.DatabaseConfigProvider
import play.api.test.WithApplication
import slick.jdbc.PostgresProfile
import utils.BuildMyApp

import scala.concurrent.Await
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class ProjectTypeSpec extends Specification with BuildMyApp {
  "ProjectType.postrunActions" should {
    "return a list of postrun actions assoicated with this project type" in new WithApplication(buildApp){
      private val injector = app.injector

      protected val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      protected implicit val db = dbConfigProvider.get[PostgresProfile].db

      val pt = Await.result(ProjectType.entryFor(1),10.seconds)
      pt must beSuccessfulTry

      val result = Await.result(pt.get.postrunActions,10.seconds)
      result must beSuccessfulTry

      result.get.length mustEqual 2

    }
  }
}
