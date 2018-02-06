import java.io.{BufferedInputStream, ByteArrayInputStream, File, FileInputStream}

import org.specs2.matcher.MatchResult
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import drivers.PathStorage
import models.StorageEntry

import scala.io.Source

@RunWith(classOf[JUnitRunner])
class PathStorageDriverSpec extends Specification with org.specs2.mock.Mockito {
  private val mock_storage = StorageEntry(None,None,"Local",None,None,None,None)

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
      s.writeDataToPath("/tmp/testfile2", testbuffer.toCharArray.map(_.toByte))

      val writtenContent = Source.fromFile("/tmp/testfile2").getLines().mkString("\n")
      writtenContent mustEqual testbuffer
    }

    "write a BufferedInputStream to file" in {
      val testbuffer = "this is my test data\nwith multiple lines"
      val s = new PathStorage(mock_storage)

      val testStream = new BufferedInputStream(new ByteArrayInputStream(testbuffer.toCharArray.map(_.toByte)))
      s.writeDataToPath("/tmp/testfile3", testStream)

      val writtenContent = Source.fromFile("/tmp/testfile3").getLines().mkString("\n")
      writtenContent mustEqual testbuffer
    }


  }
}
