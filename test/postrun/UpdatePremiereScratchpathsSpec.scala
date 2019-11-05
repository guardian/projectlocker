package postrun

import java.io.File

import helpers.PostrunDataCache
import models.{ProjectEntry, ProjectType}
import org.apache.commons.io.FileUtils
import org.specs2.mutable.Specification
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

class UpdatePremiereScratchpathsSpec extends Specification with BuildMyApp {

  "UpdatePremiereScratchpaths.postrun" should {
    "correctly read in and update a gzipped xml file" in  new WithApplication(buildApp){
      private val injector = app.injector

      protected val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      protected implicit val db = dbConfigProvider.get[PostgresProfile].db

      FileUtils.copyFileToDirectory(new File("postrun/tests/data/blank_premiere_2017.prproj"), new File("/tmp"))

      val dataCache = PostrunDataCache(Map("created_asset_folder"->"/path/to/my/assets"))
      val s = new UpdatePremiereScratchpaths
      val futureResults = Await.result(Future.sequence(Seq(
        ProjectEntry.entryForId(1),
        ProjectType.entryFor(1)
      )), 10 seconds)

      val pe = futureResults.head.asInstanceOf[Try[ProjectEntry]].get
      val pt = futureResults(1).asInstanceOf[ProjectType]

      val result = Await.result(s.postrun("/tmp/blank_premiere_2017.prproj",pe,pt,dataCache,None,None),10 seconds)
      result must beSuccessfulTry
    }
  }

  "UpdatePremiereScratchpaths.pathForClient" should {
    "replace a path starting with /srv to one starting with /Volumes" in {
      val s = new UpdatePremiereScratchpaths
      val result = s.pathForClient("/srv/test/path")
      result mustEqual "/Volumes/test/path"
    }

    "not replace a path just containing /srv" in {
      val s = new UpdatePremiereScratchpaths
      val result = s.pathForClient("/test/srv/path")
      result mustEqual "/test/srv/path"
    }

    "leave a path that does not contain /srv" in {
      val s = new UpdatePremiereScratchpaths
      val result = s.pathForClient("/a/test/path")
      result mustEqual "/a/test/path"
    }
  }
}
