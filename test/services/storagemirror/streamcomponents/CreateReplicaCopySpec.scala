package services.storagemirror.streamcomponents

import java.sql.Timestamp
import java.time.Instant

import akka.stream.{ActorMaterializer, ClosedShape, FlowShape, Materializer}
import akka.stream.scaladsl.{GraphDSL, Keep, RunnableGraph, Sink, Source}
import models.{FileEntry, StorageEntry}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import utils.AkkaTestkitSpecs2Support

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Try}

class CreateReplicaCopySpec extends Specification with Mockito {
  sequential

  def makeFileEntry() = {
    val ctime = Timestamp.from(Instant.parse("2019-01-01T00:00:00Z"))
    val mtime = Timestamp.from(Instant.parse("2019-01-02T09:10:11Z"))
    val atime = Timestamp.from(Instant.parse("2019-01-03T10:11:12Z"))

    FileEntry(Some(1),"/path/to/file",2,"someuser",3,ctime, mtime, atime, hasContent=true,hasLink=true,mirrorParent=None,lostAt=None)
  }

  "CreateReplicaCopy" should {
    "create a new file entry for the destination if none exists and yield out a ReplicaJob" in new AkkaTestkitSpecs2Support {
      implicit val mat:Materializer = ActorMaterializer.create(system)
      val destStorageMock = mock[StorageEntry]
      destStorageMock.supportsVersions returns false
      destStorageMock.id returns Some(1)

      implicit val dbMock = mock[slick.jdbc.PostgresProfile#Backend#Database]
      val mockedSavedDestFile = mock[FileEntry]
      val mockedDestFile = mock[FileEntry]
      mockedDestFile.save returns Future(mockedSavedDestFile)

      val mockCreateNewFileEntry = mock[(FileEntry,Option[Int])=>FileEntry]
      mockCreateNewFileEntry.apply(any,any) returns mockedDestFile

      val sourceFileEntry = makeFileEntry()

      val sinkFact = Sink.fold[Seq[ReplicaJob],ReplicaJob](Seq())((acc,elem)=>{
        println(s"Got $elem to add to $acc")
        acc:+elem
      })

      val graph = GraphDSL.create(sinkFact) { implicit builder=> sink=>
        import akka.stream.scaladsl.GraphDSL.Implicits._

        val src = builder.add(Source.single(sourceFileEntry).log("CreateReplicaCopySpec"))

        val toTest =  builder.add(new CreateReplicaCopy(destStorageMock){
          override def createNewFileEntry(sourceEntry: FileEntry, versionOverride: Option[Int]): FileEntry = mockCreateNewFileEntry(sourceEntry, versionOverride)
          override protected def findExistingSingle(filePath: String, destStorageId: Int, version: Int): Future[Try[Seq[FileEntry]]] = Future(Success(Seq()))
          override protected def findAllVersions(filePath: String, storageId: Int): Future[Seq[FileEntry]] = Future.failed(new RuntimeException("This should not be called"))
        })

        src ~> toTest ~> sink
        ClosedShape
      }

      val result = Await.result(RunnableGraph.fromGraph(graph).run, 10 seconds)

      there was one(mockCreateNewFileEntry).apply(sourceFileEntry, None)
      there was one(mockedDestFile).save
      result.head mustEqual ReplicaJob(sourceFileEntry, mockedSavedDestFile, None)
      result.length mustEqual 1
    }

    "use the existing file entry if one exists and versioning is not enabled on the destination storage" in new AkkaTestkitSpecs2Support {
      implicit val mat:Materializer = ActorMaterializer.create(system)
      val destStorageMock = mock[StorageEntry]
      destStorageMock.supportsVersions returns false
      destStorageMock.id returns Some(1)

      implicit val dbMock = mock[slick.jdbc.PostgresProfile#Backend#Database]
      val mockedSavedDestFile = mock[FileEntry]
      val mockedDestFile = mock[FileEntry]
      mockedDestFile.save returns Future(mockedSavedDestFile)

      val mockCreateNewFileEntry = mock[(FileEntry,Option[Int])=>FileEntry]
//      mockCreateNewFileEntry.apply(any,any) returns mockedDestFile

      val sourceFileEntry = makeFileEntry()

      val sinkFact = Sink.fold[Seq[ReplicaJob],ReplicaJob](Seq())((acc,elem)=>{
        println(s"Got $elem to add to $acc")
        acc:+elem
      })

      val graph = GraphDSL.create(sinkFact) { implicit builder=> sink=>
        import akka.stream.scaladsl.GraphDSL.Implicits._

        val src = builder.add(Source.single(sourceFileEntry).log("CreateReplicaCopySpec"))

        val toTest =  builder.add(new CreateReplicaCopy(destStorageMock){
          override def createNewFileEntry(sourceEntry: FileEntry, versionOverride: Option[Int]): FileEntry = mockCreateNewFileEntry(sourceEntry, versionOverride)
          override protected def findExistingSingle(filePath: String, destStorageId: Int, version: Int): Future[Try[Seq[FileEntry]]] = Future(Success(Seq(mockedDestFile)))
          override protected def findAllVersions(filePath: String, storageId: Int): Future[Seq[FileEntry]] = Future.failed(new RuntimeException("This should not be called"))
        })

        src ~> toTest ~> sink
        ClosedShape
      }

      val result = Await.result(RunnableGraph.fromGraph(graph).run, 10 seconds)

      there was no(mockCreateNewFileEntry).apply(any, any)
      there was one(mockedDestFile).save
      result.head mustEqual ReplicaJob(sourceFileEntry, mockedSavedDestFile, None)
      result.length mustEqual 1
    }

    "create a new file entry with an updated version field if one exists and versioning is enabled" in new AkkaTestkitSpecs2Support {
      implicit val mat:Materializer = ActorMaterializer.create(system)
      val destStorageMock = mock[StorageEntry]
      destStorageMock.supportsVersions returns true
      destStorageMock.id returns Some(1)

      implicit val dbMock = mock[slick.jdbc.PostgresProfile#Backend#Database]
      val mockedSavedDestFile = mock[FileEntry]
      val mockedExistingFile = mock[FileEntry]
      mockedExistingFile.version returns 10

      val mockedDestFile = mock[FileEntry]
      mockedDestFile.save returns Future(mockedSavedDestFile)

      val mockCreateNewFileEntry = mock[(FileEntry,Option[Int])=>FileEntry]
      mockCreateNewFileEntry.apply(any,any) returns mockedDestFile

      val sourceFileEntry = makeFileEntry()

      val sinkFact = Sink.fold[Seq[ReplicaJob],ReplicaJob](Seq())((acc,elem)=>{
        println(s"Got $elem to add to $acc")
        acc:+elem
      })

      val graph = GraphDSL.create(sinkFact) { implicit builder=> sink=>
        import akka.stream.scaladsl.GraphDSL.Implicits._

        val src = builder.add(Source.single(sourceFileEntry).log("CreateReplicaCopySpec"))

        val toTest =  builder.add(new CreateReplicaCopy(destStorageMock){
          override def createNewFileEntry(sourceEntry: FileEntry, versionOverride: Option[Int]): FileEntry = mockCreateNewFileEntry(sourceEntry, versionOverride)
          override protected def findExistingSingle(filePath: String, destStorageId: Int, version: Int): Future[Try[Seq[FileEntry]]] = Future(Success(Seq(mockedExistingFile)))
          override protected def findAllVersions(filePath: String, storageId: Int): Future[Seq[FileEntry]] = Future(Seq(mockedExistingFile))
        })

        src ~> toTest ~> sink
        ClosedShape
      }

      val result = Await.result(RunnableGraph.fromGraph(graph).run, 10 seconds)

      there was one(mockCreateNewFileEntry).apply(sourceFileEntry, Some(11))
      there was one(mockedDestFile).save
      result.head mustEqual ReplicaJob(sourceFileEntry, mockedSavedDestFile, None)
      result.length mustEqual 1
    }
  }
}
