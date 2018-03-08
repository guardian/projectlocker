import models.PostrunDependencyGraph
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import slick.jdbc.JdbcProfile
import testHelpers.TestDatabase

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[JUnitRunner])
class PostrunDependencySpec extends Specification {
  sequential

  //can over-ride bindings here. see https://www.playframework.com/documentation/2.5.x/ScalaTestingWithGuice
  private val application = new GuiceApplicationBuilder()
    .overrides(bind[DatabaseConfigProvider].to[TestDatabase.testDbProvider])
    .build
  private val injector = application.injector

  private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
  private implicit val db = dbConfigProvider.get[JdbcProfile].db

  "PostrunDependencyGraph.loadAll" should {
    "load in the dependencies table as a map of data" in {
      val result = Await.result(PostrunDependencyGraph.loadAllById,30.seconds)

      result mustEqual Map(
        1->Seq(5,6),
        4->Seq(5),
        2->Seq(1)
      )
    }
  }
}
