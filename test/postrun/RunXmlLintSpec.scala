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

class RunXmlLintSpec extends Specification with BuildMyApp{
  "RunXmlLint.postrun" should {
    "run xmllint against an existing file" in new WithApplication(buildApp){
      private val injector = app.injector

      protected val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      protected implicit val db = dbConfigProvider.get[PostgresProfile].db

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

    "fail if the given file does not exist" in new WithApplication(buildApp){
      private val injector = app.injector

      protected val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      protected implicit val db = dbConfigProvider.get[PostgresProfile].db

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

    "fail if the given file is not a gzip file" in new WithApplication(buildApp){
      private val injector = app.injector

      protected val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      protected implicit val db = dbConfigProvider.get[PostgresProfile].db

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
