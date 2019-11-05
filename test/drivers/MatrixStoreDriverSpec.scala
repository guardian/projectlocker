package drivers

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, OutputStream}

import akka.stream.Materializer
import com.om.mxs.client.japi.{MxsObject, MxsOutputStream, Vault}
import models.StorageEntry
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import scala.util.Try

class MatrixStoreDriverSpec extends Specification with Mockito {
  "MatrixStoreDriver.copyStream" should {
    "copy the entire contents of a stream" in {
      val sampleData = "this is a test, this is a test, this is a test, this is a test."
      println(s"length is ${sampleData.length}")
      val fakeInputStream = new ByteArrayInputStream(sampleData.getBytes)

      val fakeOutputStream = new ByteArrayOutputStream()

      val fakeStorageRef = mock[StorageEntry]
      implicit val fakeMaterializer:Materializer = mock[Materializer]
      val toTest = new MatrixStoreDriver(fakeStorageRef)

      val resultBytes = toTest.copyStream(fakeInputStream, fakeOutputStream, 10)

      val outputData = fakeOutputStream.toString

      outputData mustEqual sampleData
      resultBytes mustEqual sampleData.length
    }

    "work even if the file size is less than the buffer size" in {
      val sampleData = "this is a test, this is a test, this is a test, this is a test."
      val fakeInputStream = new ByteArrayInputStream(sampleData.getBytes)

      val fakeOutputStream = new ByteArrayOutputStream()

      val fakeStorageRef = mock[StorageEntry]
      implicit val fakeMaterializer:Materializer = mock[Materializer]
      val toTest = new MatrixStoreDriver(fakeStorageRef)

      val resultBytes = toTest.copyStream(fakeInputStream, fakeOutputStream, 1000000)

      val outputData = fakeOutputStream.toString

      outputData mustEqual sampleData
      resultBytes mustEqual sampleData.length
    }
  }

  "MatrixStoreDriver.writeDataToPath" should {
    "create an object if none is existing already, and write to it" in {
      val sampleData = "this is a test, this is a test, this is a test, this is a test.".getBytes

      val fakeStorageRef = mock[StorageEntry]
      implicit val fakeMaterializer:Materializer = mock[Materializer]

      val fakeOutputStream = mock[MxsOutputStream]

      val fakeFile = mock[MxsObject]
      fakeFile.newOutputStream() returns fakeOutputStream
      val fakeVault = mock[Vault]
      val mockLookupPath = mock[Function3[Vault,String,Int,Option[String]]]
      mockLookupPath.apply(any, any,any) returns None

      fakeVault.createObject(any) returns fakeFile

      val toTest = new MatrixStoreDriver(fakeStorageRef) {
        override def withVault[A](blk: Vault => Try[A]): Try[A] = blk(fakeVault)

        override def lookupPath(vault: Vault, fileName: String, version:Int): Option[String] = mockLookupPath(vault, fileName, version)
      }

      toTest.writeDataToPath("/some/path", 123, sampleData)

      there was one(mockLookupPath).apply(fakeVault,"/some/path",123)
      there was one(fakeVault).createObject(any)
      there was one(fakeOutputStream).write(sampleData)
    }

    "write an existing object if found" in {
      val sampleData = "this is a test, this is a test, this is a test, this is a test.".getBytes

      val fakeStorageRef = mock[StorageEntry]
      implicit val fakeMaterializer:Materializer = mock[Materializer]

      val fakeOutputStream = mock[MxsOutputStream]

      val fakeFile = mock[MxsObject]
      fakeFile.newOutputStream() returns fakeOutputStream
      val fakeVault = mock[Vault]
      val mockLookupPath = mock[Function3[Vault,String,Int,Option[String]]]
      mockLookupPath.apply(any, any,any) returns Some("fake-oid")

      fakeVault.getObject(any) returns fakeFile

      val toTest = new MatrixStoreDriver(fakeStorageRef) {
        override def withVault[A](blk: Vault => Try[A]): Try[A] = blk(fakeVault)

        override def lookupPath(vault: Vault, fileName: String, version:Int): Option[String] = mockLookupPath(vault, fileName, version)
      }

      toTest.writeDataToPath("/some/path", 123, sampleData)

      there was one(mockLookupPath).apply(fakeVault,"/some/path", 123)
      there was one(fakeVault).getObject("fake-oid")
      there was one(fakeOutputStream).write(sampleData)
    }
  }
}
