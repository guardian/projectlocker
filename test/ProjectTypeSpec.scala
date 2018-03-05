import org.junit.runner._
import org.specs2.runner._
import org.specs2.mutable.Specification
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import slick.jdbc.JdbcProfile
import testHelpers.TestDatabase
import models._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class ProjectTypeSpec extends Specification {
  sequential

  private val application = new GuiceApplicationBuilder()
    .overrides(bind[DatabaseConfigProvider].to[TestDatabase.testDbProvider])
    .build
  private val injector = application.injector

  private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
  private implicit val db = dbConfigProvider.get[JdbcProfile].db

  "ProjectType.postrunActions" should {
    "return a list of postrun actions assoicated with this project type" in {
      val pt = Await.result(ProjectType.entryFor(1),10.seconds)
      pt must beSuccessfulTry

      val result = Await.result(pt.get.postrunActions,10.seconds)
      result must beSuccessfulTry

      result.get.length mustEqual 1

    }
  }
}
