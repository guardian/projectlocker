import models.FileEntry
import org.specs2.mutable.Specification
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import slick.jdbc.JdbcProfile
import testHelpers.TestDatabase

import scala.concurrent.Await
import scala.util.Success
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global

class FileEntrySpec extends Specification {
  //can over-ride bindings here. see https://www.playframework.com/documentation/2.5.x/ScalaTestingWithGuice
  private val application = new GuiceApplicationBuilder()
    .overrides(bind[DatabaseConfigProvider].to[TestDatabase.testDbProvider])
    .build

  "updateFileHasContent" should {
    "update an existing database record" in {
      val injector = application.injector

      val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      implicit val db = dbConfigProvider.get[JdbcProfile].db

      val testFileEntryBeforeFuture = FileEntry.entryFor(1, db).map(_.get)
      val fileEntryBefore = Await.result(testFileEntryBeforeFuture, 10.seconds)
      fileEntryBefore.hasContent mustEqual false

      val resultFuture = testFileEntryBeforeFuture.flatMap(_.updateFileHasContent)
      val finalResult = Await.result(resultFuture, 10.seconds)

      finalResult mustEqual Success(1) //expect 1 row updated
      val testFileEntryAfterFuture = FileEntry.entryFor(1, db).map(_.get)

      val fileEntryAfter = Await.result(testFileEntryAfterFuture, 10.seconds)
      fileEntryAfter.hasContent mustEqual true
    }

  }

  "FileEntry.entryFor" should {
    "return None if the record does not exist" in {
      val injector = application.injector

      val fileController = injector.instanceOf(classOf[controllers.Files])
      val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      val db = dbConfigProvider.get[JdbcProfile].db

      val testFileEntryBeforeFuture = FileEntry.entryFor(9999, db)
      val result = Await.result(testFileEntryBeforeFuture, 10.seconds)
      result must beNone

    }
  }
}
