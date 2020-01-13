package helpers

import java.io.{File, FileInputStream}

import drivers.PathStorage
import models.{StorageEntry, StorageStatus}
import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._

import scala.io.Source
import scala.sys.process._

@RunWith(classOf[JUnitRunner])
class PathStorageDriverSpec extends Specification with org.specs2.mock.Mockito {
  sequential
  private val mock_storage = StorageEntry(None,None,None,None,"Local",None,None,None,None,None,false,Some(StorageStatus.ONLINE))

  "PathStorageDriver" should {
    "return a File object for a path" in {
      val s = new PathStorage(mock_storage)

      val response = s.fileForPath("/tmp/testfile")
      response must haveClass[File]
    }

    "write a byte array to file" in {
      val testbuffer = "this is my test data"
      val s = new PathStorage(mock_storage)

      //this method is blocking
      s.writeDataToPath("/tmp/testfile2", 123, testbuffer.toCharArray.map(_.toByte))

      val writtenContent = Source.fromFile("/tmp/testfile2").getLines().mkString("\n")
      writtenContent mustEqual testbuffer
    }

    "write an InputStream to file" in {
      val s = new PathStorage(mock_storage)

      val testFile=new FileInputStream(new File("public/images/uploading.svg"))

      s.writeDataToPath("/tmp/testfile3",123, testFile)

      val checksumSource = "shasum -a 1 public/images/uploading.svg" #| "cut -c 1-40" !!
      val checksumDest = "shasum -a 1 /tmp/testfile3" #| "cut -c 1-40" !!

      checksumDest mustEqual checksumSource
    }

    "delete an existing file" in {
      val s= new PathStorage(mock_storage)

      val testFile=new FileInputStream(new File("public/images/uploading.svg"))
      s.writeDataToPath("/tmp/testfile4", 123, testFile)

      val fileBefore = new File("/tmp/testfile4")
      fileBefore.exists must beTrue

      s.deleteFileAtPath("/tmp/testfile4", 123)

      val fileAfter = new File("/tmp/testfile4")
      fileAfter.exists must beFalse
    }

    "return file metadata" in {
      val testbuffer = "this is my test data"
      val s = new PathStorage(mock_storage)

      //this method is blocking
      s.writeDataToPath("/tmp/testfile5", 123, testbuffer.toCharArray.map(_.toByte))

      val metaDict = s.getMetadata("/tmp/testfile5", 123)
      metaDict.get('size) must beSome("20")
      metaDict.get('lastModified) must beSome[String]
      metaDict.get('zzzzz) must beNone
    }

    "not support version numbers" in {
      val s = new PathStorage(mock_storage)

      s.supportsVersions mustEqual false
    }
  }
}
