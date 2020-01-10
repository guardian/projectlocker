package helpers

import java.io._
import java.sql.Timestamp
import java.time.LocalDateTime

import akka.stream.Materializer
import drivers.{PathStorage, StorageDriver}
import models.{FileEntry, StorageEntry}
import org.apache.commons.io.input.NullInputStream
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.{JdbcBackend, JdbcProfile}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.sys.process._
import scala.util.{Failure, Success, Try}
import play.api.test.WithApplication
import org.apache.commons.io.FilenameUtils

class StorageHelperSpec extends Specification with Mockito with utils.BuildMyApp {
  "StorageHelper.copyStream" should {
    "reliably copy one stream to another, returning the number of bytes copied" in {
      val testFileNameSrc = "/tmp/storageHelperSpecTest-src-1" // shouldn't have spaces!
      val testFileNameDest = "/tmp/storageHelperSpecTest-dst-1"

      /* create a test file */
      Seq("/bin/dd","if=/dev/urandom",s"of=$testFileNameSrc","bs=1k","count=600").!

      val checksumSource = Seq("shasum","-a","1",testFileNameSrc) #| "cut -c 1-40" !!

      val sourceFile = new File(testFileNameSrc)
      val sourceStream = new FileInputStream(sourceFile)

      val destFile = new File(testFileNameDest)
      if(destFile.exists()) destFile.delete()

      val destStream = new FileOutputStream(destFile)

      implicit val mat:Materializer = mock[Materializer]
      val h = new StorageHelper
      val result = h.copyStream(sourceStream,destStream)

      result mustEqual sourceFile.length
      val checksumDest = s"shasum -a 1 $testFileNameDest" #| "cut -c 1-40" !!

      checksumSource mustEqual checksumDest

      sourceStream.close()
      destStream.close()
      sourceFile.delete()
      destFile.delete()
    }
  }

  "StorageHelper.copyFile" should {
    "look up two file entries, get streams from their device drivers and initiate copy" in new WithApplication(buildApp) {
      val injector = app.injector
      protected val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      protected implicit val db = dbConfigProvider.get[JdbcProfile].db

      val testFileNameSrc = "/tmp/storageHelperSpecTest-src-2" // shouldn't have spaces!
      val testFileNameDest = "/tmp/storageHelperSpecTest-dst-2" // shouldn't have spaces!
      try {
        // create a test file
        Logger.debug( Seq("/bin/dd", "if=/dev/urandom", s"of=$testFileNameSrc", "bs=1k", "count=600").toString())
        Seq("/bin/dd", "if=/dev/urandom", s"of=$testFileNameSrc", "bs=1k", "count=600").!
        Seq("/bin/ls", "-lh", testFileNameSrc).!

        val ts = Timestamp.valueOf(LocalDateTime.now())

        val testSourceEntry = FileEntry(None, FilenameUtils.getBaseName(testFileNameSrc), 1, "testuser", 1, ts, ts, ts, hasContent = true, hasLink = false)
        val testDestEntry = FileEntry(None, FilenameUtils.getBaseName(testFileNameDest), 1, "testuser", 1, ts, ts, ts, hasContent = false, hasLink = false)

        val realStorageHelper = new StorageHelper

        val savedResults = Await.result(
          Future.sequence(Seq(testSourceEntry.save, testDestEntry.save)).map(results => Try(results.map(_.get)))
          , 10.seconds)

        savedResults must beSuccessfulTry

        val savedSource = savedResults.get.head
        val savedDest = savedResults.get(1)
        val result = Await.result(realStorageHelper.copyFile(savedSource, savedDest), 10.seconds)
        result must beRight(savedDest.copy(hasContent = true))
      } finally { // ensure that test files get deleted. if you don't use try/finally, then if either of these fails the whole test does
        new File(testFileNameSrc).delete()
        new File(testFileNameDest).delete()
      }
    }

    "fail if the destination file is not the same size as the source" in new WithApplication(buildApp){
      val injector = app.injector
      protected val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      protected implicit val db = dbConfigProvider.get[JdbcProfile].db

      val mockedStorageDriver = mock[PathStorage]
      mockedStorageDriver.getReadStream(any[String],any)
      mockedStorageDriver.getReadStream(any[String],any) answers((_,_)=>Success(new NullInputStream(60*1024L)))
      mockedStorageDriver.getMetadata(any[String],any) answers((_,_)=>Map('size->"1234"))

      val mockedStorage = mock[StorageEntry]
      mockedStorage.getStorageDriver answers((_,_)=>{println("in mockedStorage"); Some(mockedStorageDriver)})
      mockedStorage.rootpath answers((_,_)=>Some("/tmp"))

      val testFileNameSrc = "/tmp/storageHelperSpecTest-src-4" // shouldn't have spaces!
      val testFileNameDest = "/tmp/storageHelperSpecTest-dst-4" // shouldn't have spaces!
      try {
        // create a test file
        Seq("/bin/dd", "if=/dev/urandom", s"of=$testFileNameSrc", "bs=1k", "count=600").!
        val ts = Timestamp.valueOf(LocalDateTime.now())

        val testSourceEntry = new FileEntry(Some(1234), FilenameUtils.getBaseName(testFileNameSrc), 1, "testuser", 1, ts, ts, ts, hasContent = true, hasLink = false){
          override def storage(implicit db: JdbcBackend#DatabaseDef):Future[Option[StorageEntry]] = {
            println("testSourceEntry.storage")
            Future(Some(mockedStorage))
          }
        }

        val testDestEntry = FileEntry(None, FilenameUtils.getBaseName(testFileNameDest), 1, "testuser", 1, ts, ts, ts, hasContent = false, hasLink = false)

        val realStorageHelper = new StorageHelper

        val savedResults = Await.result(
          Future.sequence(Seq(testDestEntry.save)).map(results => Try(results.map(_.get)))
          , 10.seconds)

        savedResults must beSuccessfulTry

        val savedSource = testSourceEntry
        val savedDest = savedResults.get.head
        println(savedSource)
        println(savedDest)
        val result = Await.result(realStorageHelper.copyFile(savedSource, savedDest), 10.seconds)
        //intellij does not like this line, but the compiler does
        result must beLeft(List("Copied file byte size 61440 did not match source file 1234"))
      } finally { // ensure that test files get deleted. if you don't use try/finally, then if either of these fails the whole test does
        new File(testFileNameSrc).delete()
        new File(testFileNameDest).delete()
      }
    }

    "return an error if source does not have a valid storage driver" in new WithApplication(buildApp){
      val injector = app.injector
      protected val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      protected implicit val db = dbConfigProvider.get[JdbcProfile].db

      // create a test file
      val testFileNameSrc = "/tmp/storageHelperSpecTest-src-3" // shouldn't have spaces!
      val testFileNameDest = "/tmp/storageHelperSpecTest-dst-3" // shouldn't have spaces!
      try {
        Seq("/bin/dd", "if=/dev/urandom", s"of=$testFileNameSrc", "bs=1k", "count=600").!
        val ts = Timestamp.valueOf(LocalDateTime.now())

        val testSourceEntry = FileEntry(None, FilenameUtils.getBaseName(testFileNameSrc), 2, "testuser", 1, ts, ts, ts, hasContent = true, hasLink = false)
        val testDestEntry = FileEntry(None, FilenameUtils.getBaseName(testFileNameSrc), 1, "testuser", 1, ts, ts, ts, hasContent = false, hasLink = false)

        val realStorageHelper = new StorageHelper

        val savedResults = Await.result(
          Future.sequence(Seq(testSourceEntry.save, testDestEntry.save)).map(results => Try(results.map(_.get)))
          , 10.seconds)

        savedResults must beSuccessfulTry

        val savedSource = savedResults.get.head
        val savedDest = savedResults.get(1)
        val result = Await.result(realStorageHelper.copyFile(savedSource, savedDest), 10.seconds)
        result mustEqual Left(List("Either source or destination was missing a storage or a storage driver"))
      } finally {
        new File(testFileNameSrc).delete()
        new File(testFileNameDest).delete()
      }
    }

    "return an error if dest does not have a valid storage driver" in new WithApplication(buildApp){
      val injector = app.injector
      protected val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      protected implicit val db = dbConfigProvider.get[JdbcProfile].db
      // create a test file
      val testFileNameSrc = "/tmp/storageHelperSpecTest-src-5" // shouldn't have spaces!
      val testFileNameDest = "/tmp/storageHelperSpecTest-dst-5" // shouldn't have spaces!
      try {
        Seq("/bin/dd", "if=/dev/urandom", s"of=$testFileNameSrc", "bs=1k", "count=600").!
        val ts = Timestamp.valueOf(LocalDateTime.now())

        val testSourceEntry = FileEntry(None, FilenameUtils.getBaseName(testFileNameSrc), 1, "testuser", 1, ts, ts, ts, hasContent = true, hasLink = false)
        val testDestEntry = FileEntry(None, FilenameUtils.getBaseName(testFileNameDest), 2, "testuser", 1, ts, ts, ts, hasContent = false, hasLink = false)

        val realStorageHelper = new StorageHelper

        val savedResults = Await.result(
          Future.sequence(Seq(testSourceEntry.save, testDestEntry.save)).map(results => Try(results.map(_.get)))
          , 10.seconds)

        savedResults must beSuccessfulTry

        val savedSource = savedResults.get.head
        val savedDest = savedResults.get(1)
        val result = Await.result(realStorageHelper.copyFile(savedSource, savedDest), 10.seconds)
        result mustEqual Left(List("Either source or destination was missing a storage or a storage driver"))
      } finally {
        new File(testFileNameSrc).delete()
        new File(testFileNameDest).delete()
      }
    }

  }

  "StorageHelper.doByteCopy" should {
    "return an error if getting the source stream failed" in {
      val mockStorageDriver = mock[PathStorage]

      val sourceStreamTry = Failure(new RuntimeException("Kaboom!"))
      val destStreamTry = Success(mock[FileOutputStream])
      implicit val mat:Materializer = mock[Materializer]
      val h = new StorageHelper {
        def testDoByteCopy(sourceStorageDriver:StorageDriver,
                           sourceStreamTry:Try[InputStream], destStreamTry:Try[OutputStream],
                           sourceFullPath:String, sourceVersion:Int, destFullPath: String) =
          doByteCopy(sourceStorageDriver,sourceStreamTry,destStreamTry,sourceFullPath,sourceVersion, destFullPath)
      }

      val result = h.testDoByteCopy(mockStorageDriver, sourceStreamTry,destStreamTry,"/source",123,"/dest")

      result must beLeft
      println(result.left.get)
      result.left.get.length mustEqual 2
      result.left.get.head mustEqual "java.lang.RuntimeException: Kaboom!"
      result.left.get(1) mustEqual ""
    }

    "return an error if getting the destination stream failed" in {
      val mockStorageDriver = mock[PathStorage]

      val sourceStreamTry = Success(mock[FileInputStream])
      val destStreamTry = Failure(new RuntimeException("Kaboom!"))
      implicit val mat:Materializer = mock[Materializer]
      val h = new StorageHelper {
        /**
          * helper to call through to protected method
          */
        def testDoByteCopy(sourceStorageDriver:StorageDriver,
                           sourceStreamTry:Try[InputStream], destStreamTry:Try[OutputStream],
                           sourceFullPath:String, sourceVersion:Int, destFullPath: String) =
          doByteCopy(sourceStorageDriver,sourceStreamTry,destStreamTry,sourceFullPath,sourceVersion, destFullPath)
      }

      val result = h.testDoByteCopy(mockStorageDriver, sourceStreamTry,destStreamTry,"/source",123,"/dest")

      result must beLeft
      println(result.left.get)
      result.left.get.length mustEqual 2
      result.left.get.head mustEqual ""
      result.left.get(1) mustEqual "java.lang.RuntimeException: Kaboom!"
    }

    "catch and pass along any exception thrown from copyStream as a Left stringvalue" in {
      val mockStorageDriver = mock[PathStorage]

      val sourceStreamTry = Success(mock[FileInputStream])
      val destStreamTry = Success(mock[FileOutputStream])

      implicit val mat:Materializer = mock[Materializer]
      val h = new StorageHelper {
        /**
          * stub implementation to throw exception
          */
        override def copyStream(source: InputStream, dest: OutputStream, chunkSize: Int): Int = {
          throw new RuntimeException("**raspberry**")
        }
        /**
          * helper to call through to protected method
          */
        def testDoByteCopy(sourceStorageDriver:StorageDriver,
                           sourceStreamTry:Try[InputStream], destStreamTry:Try[OutputStream],
                           sourceFullPath:String, sourceVersion:Int, destFullPath: String) =
          doByteCopy(sourceStorageDriver,sourceStreamTry,destStreamTry,sourceFullPath,sourceVersion, destFullPath)
      }

      val result = h.testDoByteCopy(mockStorageDriver, sourceStreamTry,destStreamTry,"/source",123,"/dest")

      result must beLeft
      println(result.left.get)
      result.left.get.length mustEqual 1
      result.left.get.head mustEqual "java.lang.RuntimeException: **raspberry**"
    }
  }
}
