package postrun

import java.io.File

import helpers.PostrunDataCache
import models.{ProjectEntry, ProjectType}
import org.apache.commons.io.FileUtils
import org.specs2.mutable.Specification
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.WithApplication
import slick.jdbc.{JdbcProfile, PostgresProfile}
import testHelpers.TestDatabase
import utils.BuildMyApp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Try

class UpdateAdobeUuidSpec extends Specification with BuildMyApp {
  private val logger=Logger(getClass)

  "UpdateAdobeUuid.postrun" should {
    "correctly read in and update a gzipped xml file" in  new WithApplication(buildApp){
      private val injector = app.injector

      protected val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      protected implicit val db = dbConfigProvider.get[PostgresProfile].db

      FileUtils.copyFile(new File("postrun/tests/data/blank_premiere_2017.prproj"), new File("/tmp/test_update_uuid.prproj"))

      val dataCache = PostrunDataCache(Map())
      val s = new UpdateAdobeUuid
      val futureResults = Await.result(Future.sequence(Seq(
        ProjectEntry.entryForId(1),
        ProjectType.entryFor(1)
      )), 10 seconds)

      val pe = futureResults.head.asInstanceOf[Try[ProjectEntry]].get
      val pt = futureResults(1).asInstanceOf[ProjectType]

      val result = Await.result(s.postrun("/tmp/test_update_uuid.prproj",pe,pt,dataCache,None,None),10 seconds)
      result must beSuccessfulTry
      val updateDataCache = result.get
      logger.info(s"Updated adobe uuid is ${updateDataCache.get("new_adobe_uuid")}")
      updateDataCache.get("new_adobe_uuid") must beSome
    }
  }
}
