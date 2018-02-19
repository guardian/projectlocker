import java.sql.Timestamp
import java.time.LocalDateTime

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

class FileEntrySpec extends Specification with TestDatabaseAccess {

  "updateFileHasContent" should {
    "update an existing database record if an id exists" in {
      val testFileEntryBeforeFuture = FileEntry.entryFor(1, db).map(_.get)
      val fileEntryBefore = Await.result(testFileEntryBeforeFuture, 10.seconds)
      fileEntryBefore.hasContent mustEqual false

      val resultFuture = testFileEntryBeforeFuture.flatMap(_.updateFileHasContent)
      val finalResult = Await.result(resultFuture, 10.seconds)

      finalResult must beSuccessfulTry(1) //expect 1 row updated
      val testFileEntryAfterFuture = FileEntry.entryFor(1, db).map(_.get)

      val fileEntryAfter = Await.result(testFileEntryAfterFuture, 10.seconds)
      fileEntryAfter.hasContent mustEqual true
    }

    "return a failure if there is no id set in the record" in {
      val ts = Timestamp.valueOf(LocalDateTime.now())
      val testFileEntryBefore = FileEntry(None,"notexistingtestfile",1,"test-user",1,ts,ts,ts,false,false)

      val resultFuture = testFileEntryBefore.updateFileHasContent
      val finalResult = Await.result(resultFuture, 10.seconds)

      finalResult must beFailedTry
      finalResult.toEither.left.get.toString mustEqual "java.lang.RuntimeException: Can't update a file record that has not been saved"
    }
  }

  "save" should {
    "insert a non-existing file into the database" in {
      val ts = Timestamp.valueOf(LocalDateTime.now())
      val testFileEntry = FileEntry(None,"/path/to/nonexisting",1,"test-user",1,ts,ts,ts,false,false)

      val result = Await.result(testFileEntry.save, 10.seconds)
      result must beSuccessfulTry
      result.get.id must beSome //ensure that the ID has been set

      val testEntryRead = Await.result(FileEntry.entryFor(result.get.id.get,db),10.seconds)
      testEntryRead must beSome
      testEntryRead.get.filepath mustEqual "/path/to/nonexisting"
      testEntryRead.get.storageId mustEqual 1
      testEntryRead.get.user mustEqual "test-user"
      testEntryRead.get.mtime mustEqual ts
    }

    "update a pre-existing file in the database" in {
      val testFileEntry = Await.result(FileEntry.entryFor(4,db),10.seconds)
      val ts = Timestamp.valueOf(LocalDateTime.now())
      testFileEntry must beSome
      testFileEntry.get.id must beSome(4)
      testFileEntry.get.filepath mustEqual "/tmp/testprojectfile"
      testFileEntry.get.user mustEqual "you"
      testFileEntry.get.mtime mustEqual Timestamp.valueOf("2016-12-11 12:21:11.021")
      val fileEntryUpdated = testFileEntry.get.copy(user="test-user", mtime = ts)

      val updateResult = Await.result(fileEntryUpdated.save,10.seconds)
      updateResult must beSuccessfulTry

      val testEntryRead = Await.result(FileEntry.entryFor(4,db),10.seconds)

      testEntryRead must beSome
      testEntryRead.get.id must beSome(4)
      testEntryRead.get.filepath mustEqual "/tmp/testprojectfile"
      testEntryRead.get.mtime mustEqual ts
      testEntryRead.get.user mustEqual "test-user"
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
