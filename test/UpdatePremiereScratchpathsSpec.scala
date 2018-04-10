import java.io.File

import helpers.PostrunDataCache
import models.{ProjectEntry, ProjectType}
import org.apache.commons.io.FileUtils
import org.specs2.mutable.Specification
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import postrun.UpdatePremiereScratchpaths
import slick.jdbc.JdbcProfile
import testHelpers.TestDatabase

import scala.concurrent.{Await, Future}
import scala.util.Try
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class UpdatePremiereScratchpathsSpec extends Specification {
  protected val application = new GuiceApplicationBuilder()
    .overrides(bind[DatabaseConfigProvider].to[TestDatabase.testDbProvider])
    .build

  private val injector = application.injector

  protected val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
  protected implicit val db = dbConfigProvider.get[JdbcProfile].db

  "UpdatePremiereScratchpaths.postrun" should {
    "correctly read in and update a gzipped xml file" in {
      FileUtils.copyFileToDirectory(new File("postrun/tests/data/blank_premiere_2017.prproj"), new File("/tmp"))

      val dataCache = PostrunDataCache(Map("created_asset_folder"->"/path/to/my/assets"))
      val s = new UpdatePremiereScratchpaths
      val futureResults = Await.result(Future.sequence(Seq(
        ProjectEntry.entryForId(1),
        ProjectType.entryFor(1)
      )), 10 seconds)

      val pe = futureResults.head.asInstanceOf[Try[ProjectEntry]].get
      val pt = futureResults(1).asInstanceOf[Try[ProjectType]].get

      val result = Await.result(s.postrun("/tmp/blank_premiere_2017.prproj",pe,pt,dataCache,None,None),10 seconds)
      result must beSuccessfulTry
    }
  }
}
