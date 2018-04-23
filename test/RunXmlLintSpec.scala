import java.io.File

import helpers.PostrunDataCache
import models.{ProjectEntry, ProjectType}
import org.apache.commons.io.FileUtils
import org.specs2.mutable.Specification
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import postrun.RunXmlLint
import slick.jdbc.JdbcProfile
import testHelpers.TestDatabase

import scala.concurrent.{Await, Future}
import scala.util.Try
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class RunXmlLintSpec extends Specification {
  protected val application = new GuiceApplicationBuilder()
    .overrides(bind[DatabaseConfigProvider].to[TestDatabase.testDbProvider])
    .build

  private val logger = Logger(getClass)
  private val injector = application.injector

  protected val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
  protected implicit val db = dbConfigProvider.get[JdbcProfile].db

  "RunXmlLint.postrun" should {
    "run xmllint against an existing file" in {
      FileUtils.copyFile(new File("postrun/tests/data/blank_premiere_2017.prproj"), new File("/tmp/test_run_xmllint.prproj"))

      val dataCache = PostrunDataCache(Map())
      val s = new RunXmlLint
      val futureResults = Await.result(Future.sequence(Seq(
        ProjectEntry.entryForId(1),
        ProjectType.entryFor(1)
      )), 10 seconds)

      val pe = futureResults.head.asInstanceOf[Try[ProjectEntry]].get
      val pt = futureResults(1).asInstanceOf[Try[ProjectType]].get

      val result = Await.result(s.postrun("/tmp/test_run_xmllint.prproj",pe,pt,dataCache,None,None),10 seconds)
      result must beSuccessfulTry
    }

    "fail if the given file does not exist" in {
      val dataCache = PostrunDataCache(Map())
      val s = new RunXmlLint
      val futureResults = Await.result(Future.sequence(Seq(
        ProjectEntry.entryForId(1),
        ProjectType.entryFor(1)
      )), 10 seconds)

      val pe = futureResults.head.asInstanceOf[Try[ProjectEntry]].get
      val pt = futureResults(1).asInstanceOf[Try[ProjectType]].get

      val result = Await.result(s.postrun("/tmp/fdsjnfsnmbsfnNOTAFILE.prproj",pe,pt,dataCache,None,None),10 seconds)
      result must beFailedTry
    }

    "fail if the given file is not a gzip file" in {
      FileUtils.copyFile(new File("postrun/tests/test_make_asset_folder.py"), new File("/tmp/test_run_xmllint_notgzip.prproj"))
      val dataCache = PostrunDataCache(Map())
      val s = new RunXmlLint
      val futureResults = Await.result(Future.sequence(Seq(
        ProjectEntry.entryForId(1),
        ProjectType.entryFor(1)
      )), 10 seconds)

      val pe = futureResults.head.asInstanceOf[Try[ProjectEntry]].get
      val pt = futureResults(1).asInstanceOf[Try[ProjectType]].get

      val result = Await.result(s.postrun("/tmp/test_run_xmllint_notgzip.prproj",pe,pt,dataCache,None,None),10 seconds)
      result must beFailedTry
    }
  }
}
