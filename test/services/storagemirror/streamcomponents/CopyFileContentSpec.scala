package services.storagemirror.streamcomponents

import akka.stream.{ActorMaterializer, FlowShape, Materializer}
import drivers.StorageDriver
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import utils.AkkaTestkitSpecs2Support
import java.io.{InputStream, OutputStream}
import java.sql.Timestamp
import java.time.Instant

import akka.stream.scaladsl.{GraphDSL, Keep, Sink, Source}
import models.{FileEntry, StorageEntry}

import scala.concurrent.Await
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._

class CopyFileContentSpec extends Specification with Mockito {
  sequential

  def makeFileEntry(fakeFileName:String) = {
    val ctime = Timestamp.from(Instant.parse("2019-01-01T00:00:00Z"))
    val mtime = Timestamp.from(Instant.parse("2019-01-02T09:10:11Z"))
    val atime = Timestamp.from(Instant.parse("2019-01-03T10:11:12Z"))

    FileEntry(Some(1),fakeFileName,2,"someuser",3,ctime, mtime, atime, hasContent=true,hasLink=true,mirrorParent=None,lostAt=None)
  }


  "CopyFileContent" should {
    "perform a copy by obtaining input and output streams from the relevant storage drivers and calling the internal method" in new AkkaTestkitSpecs2Support {
      implicit val mat:Materializer = ActorMaterializer.create(system)

      val mockSourceStream = mock[InputStream]
      val sourceStorageDriverMock = mock[StorageDriver]
      sourceStorageDriverMock.getReadStream(any, any) returns Success(mockSourceStream)

      val mockDestStream = mock[OutputStream]
      val destStorageDriverMock = mock[StorageDriver]
      destStorageDriverMock.getWriteStream(any,any) returns Success(mockDestStream)

      val sourceStorageMock = mock[StorageEntry]
      sourceStorageMock.getStorageDriver returns Some(sourceStorageDriverMock)
      sourceStorageMock.fullPathFor(any) returns "/root/path/to/fakeSourceFile"

      val destStorageMock = mock[StorageEntry]
      destStorageMock.getStorageDriver returns Some(destStorageDriverMock)
      destStorageMock.fullPathFor(any) returns "fakeDestFile"

      val copyFunctionMock = mock[(InputStream,OutputStream)=>Try[Long]]
      copyFunctionMock.apply(any,any) returns Success(123456L)

      val elemToTest = GraphDSL.create() { implicit builder=>
        val elem = builder.add(new CopyFileContent(sourceStorageMock,destStorageMock){
          override protected def doCopy(sourceStream: InputStream, destStream: OutputStream): Try[Long] = copyFunctionMock.apply(sourceStream, destStream)
        })

        FlowShape(elem.in, elem.out)
      }

      val incomingJob = ReplicaJob(makeFileEntry("/path/to/fakeSourceFile"), makeFileEntry("fakeDestFile"),None)

      val graph = Source.single(incomingJob).via(elemToTest).toMat(Sink.fold[Seq[ReplicaJob],ReplicaJob](Seq())((acc,elem)=>acc:+elem))(Keep.right)

      val result = Await.result(graph.run(), 10 seconds)

      result.length mustEqual 1
      result.head.bytesCopied must beSome(123456L)

      there was one(copyFunctionMock).apply(mockSourceStream,mockDestStream)
      there was one(sourceStorageMock).fullPathFor("/path/to/fakeSourceFile")
      there was one(destStorageMock).fullPathFor("fakeDestFile")
    }

    "throw an exception if there is no source storage driver" in new AkkaTestkitSpecs2Support {
      implicit val mat:Materializer = ActorMaterializer.create(system)

      val mockSourceStream = mock[InputStream]
      val sourceStorageDriverMock = mock[StorageDriver]
      sourceStorageDriverMock.getReadStream(any, any) returns Success(mockSourceStream)

      val mockDestStream = mock[OutputStream]
      val destStorageDriverMock = mock[StorageDriver]
      destStorageDriverMock.getWriteStream(any,any) returns Success(mockDestStream)

      val sourceStorageMock = mock[StorageEntry]
      sourceStorageMock.getStorageDriver returns None
      sourceStorageMock.fullPathFor(any) returns "/root/path/to/fakeSourceFile"

      val destStorageMock = mock[StorageEntry]
      destStorageMock.getStorageDriver returns Some(destStorageDriverMock)
      destStorageMock.fullPathFor(any) returns "fakeDestFile"

      val copyFunctionMock = mock[(InputStream,OutputStream)=>Try[Long]]
      copyFunctionMock.apply(any,any) returns Success(123456L)

      val elemToTest = GraphDSL.create() { implicit builder=>
        val elem = builder.add(new CopyFileContent(sourceStorageMock,destStorageMock){
          override protected def doCopy(sourceStream: InputStream, destStream: OutputStream): Try[Long] = copyFunctionMock.apply(sourceStream, destStream)
        })

        FlowShape(elem.in, elem.out)
      }

      val incomingJob = ReplicaJob(makeFileEntry("/path/to/fakeSourceFile"), makeFileEntry("fakeDestFile"),None)

      val graph = Source.single(incomingJob).via(elemToTest).toMat(Sink.fold[Seq[ReplicaJob],ReplicaJob](Seq())((acc,elem)=>acc:+elem))(Keep.right)

      def theTest = {
        val result = Await.result(graph.run(), 10 seconds)

      }
      theTest must throwA[RuntimeException]

      there was no(copyFunctionMock).apply(mockSourceStream,mockDestStream)
      there was no(sourceStorageMock).fullPathFor("/path/to/fakeSourceFile")
      there was no(destStorageMock).fullPathFor("fakeDestFile")
    }

    "throw an exception if there is no destination storage driver" in new AkkaTestkitSpecs2Support {
      implicit val mat:Materializer = ActorMaterializer.create(system)

      val mockSourceStream = mock[InputStream]
      val sourceStorageDriverMock = mock[StorageDriver]
      sourceStorageDriverMock.getReadStream(any, any) returns Success(mockSourceStream)

      val mockDestStream = mock[OutputStream]
      val destStorageDriverMock = mock[StorageDriver]
      destStorageDriverMock.getWriteStream(any,any) returns Success(mockDestStream)

      val sourceStorageMock = mock[StorageEntry]
      sourceStorageMock.getStorageDriver returns Some(sourceStorageDriverMock)
      sourceStorageMock.fullPathFor(any) returns "/root/path/to/fakeSourceFile"

      val destStorageMock = mock[StorageEntry]
      destStorageMock.getStorageDriver returns None
      destStorageMock.fullPathFor(any) returns "fakeDestFile"

      val copyFunctionMock = mock[(InputStream,OutputStream)=>Try[Long]]
      copyFunctionMock.apply(any,any) returns Success(123456L)

      val elemToTest = GraphDSL.create() { implicit builder=>
        val elem = builder.add(new CopyFileContent(sourceStorageMock,destStorageMock){
          override protected def doCopy(sourceStream: InputStream, destStream: OutputStream): Try[Long] = copyFunctionMock.apply(sourceStream, destStream)
        })

        FlowShape(elem.in, elem.out)
      }

      val incomingJob = ReplicaJob(makeFileEntry("/path/to/fakeSourceFile"), makeFileEntry("fakeDestFile"),None)

      val graph = Source.single(incomingJob).via(elemToTest).toMat(Sink.fold[Seq[ReplicaJob],ReplicaJob](Seq())((acc,elem)=>acc:+elem))(Keep.right)

      def theTest = {
        val result = Await.result(graph.run(), 10 seconds)

      }
      theTest must throwA[RuntimeException]

      there was no(copyFunctionMock).apply(mockSourceStream,mockDestStream)
      there was no(sourceStorageMock).fullPathFor("/path/to/fakeSourceFile")
      there was no(destStorageMock).fullPathFor("fakeDestFile")
    }

    "abort if the copy operation throws an exception" in new AkkaTestkitSpecs2Support {
      implicit val mat:Materializer = ActorMaterializer.create(system)

      val mockSourceStream = mock[InputStream]
      val sourceStorageDriverMock = mock[StorageDriver]
      sourceStorageDriverMock.getReadStream(any, any) returns Success(mockSourceStream)

      val mockDestStream = mock[OutputStream]
      val destStorageDriverMock = mock[StorageDriver]
      destStorageDriverMock.getWriteStream(any,any) returns Success(mockDestStream)

      val sourceStorageMock = mock[StorageEntry]
      sourceStorageMock.getStorageDriver returns Some(sourceStorageDriverMock)
      sourceStorageMock.fullPathFor(any) returns "/root/path/to/fakeSourceFile"

      val destStorageMock = mock[StorageEntry]
      destStorageMock.getStorageDriver returns Some(destStorageDriverMock)
      destStorageMock.fullPathFor(any) returns "fakeDestFile"

      val copyFunctionMock = mock[(InputStream,OutputStream)=>Try[Long]]
      copyFunctionMock.apply(any,any) returns Failure(new java.io.IOException("kaboom"))

      val elemToTest = GraphDSL.create() { implicit builder=>
        val elem = builder.add(new CopyFileContent(sourceStorageMock,destStorageMock){
          override protected def doCopy(sourceStream: InputStream, destStream: OutputStream): Try[Long] = copyFunctionMock.apply(sourceStream, destStream)
        })

        FlowShape(elem.in, elem.out)
      }

      val incomingJob = ReplicaJob(makeFileEntry("/path/to/fakeSourceFile"), makeFileEntry("fakeDestFile"),None)

      val graph = Source.single(incomingJob).via(elemToTest).toMat(Sink.fold[Seq[ReplicaJob],ReplicaJob](Seq())((acc,elem)=>acc:+elem))(Keep.right)

      def theTest = {
        Await.result(graph.run(), 10 seconds)
      }
      theTest must throwA[java.io.IOException]

      there was one(copyFunctionMock).apply(mockSourceStream,mockDestStream)
      there was one(sourceStorageMock).fullPathFor("/path/to/fakeSourceFile")
      there was one(destStorageMock).fullPathFor("fakeDestFile")
    }

  }
}
