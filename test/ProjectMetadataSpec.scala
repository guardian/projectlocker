import helpers.PostrunDataCache
import models.{ProjectEntry, ProjectMetadata, ProjectType}
import org.apache.commons.io.FileUtils
import org.specs2.mutable.Specification
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import slick.jdbc.JdbcProfile
import testHelpers.TestDatabase

import scala.concurrent.{Await, Future}
import scala.util.Try
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class ProjectMetadataSpec extends Specification {
  protected val application = new GuiceApplicationBuilder()
    .overrides(bind[DatabaseConfigProvider].to[TestDatabase.testDbProvider])
    .build

  private val logger = Logger(getClass)
  private val injector = application.injector

  protected val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
  protected implicit val db = dbConfigProvider.get[JdbcProfile].db

  "ProjectMetadata.setBulk" should {
    "add a Map of keys to the database" in {
      val result = Await.result(ProjectMetadata.setBulk(4,Map("key"->"value","key_two"->"value_two")),10 seconds)

      result must beSuccessfulTry(2)
    }
  }
}
