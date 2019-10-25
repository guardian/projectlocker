package services.storagemirror.streamcomponents

import java.sql.Timestamp
import java.time.Instant

import akka.stream.scaladsl.{GraphDSL, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, ClosedShape, FlowShape, Materializer}
import drivers.StorageDriver
import models.{FileEntry, StorageEntry}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import utils.AkkaTestkitSpecs2Support

import scala.concurrent.Await
import scala.concurrent.duration._

class FileExistsSwitchSpec extends Specification with Mockito {
  def makeFileEntry() = {
    val ctime = Timestamp.from(Instant.parse("2019-01-01T00:00:00Z"))
    val mtime = Timestamp.from(Instant.parse("2019-01-02T09:10:11Z"))
    val atime = Timestamp.from(Instant.parse("2019-01-03T10:11:12Z"))

    FileEntry(Some(1),"/path/to/file",2,"someuser",3,ctime, mtime, atime, hasContent=true,hasLink=true,mirrorParent=None,lostAt=None)
  }

  "FileExistsSwitch" should {
    "push to YES if the storage driver reports that the file exists" in new AkkaTestkitSpecs2Support {
      implicit val mat:Materializer = ActorMaterializer.create(system)

      val mockedStorageDriver = mock[StorageDriver]
      mockedStorageDriver.pathExists(any,any) returns true
      val mockedStorageRef = mock[StorageEntry]
      mockedStorageRef.getStorageDriver returns Some(mockedStorageDriver)
      mockedStorageRef.fullPathFor(any) returns "/full/path/to/file"

      val sinkFact = Sink.fold[Seq[FileEntry],FileEntry](Seq())((acc,elem)=>acc:+elem)

      val mockedSourcefile = makeFileEntry()
      val graph = GraphDSL.create(sinkFact) { implicit builder=> sink=>
        import akka.stream.scaladsl.GraphDSL.Implicits._

        val src = builder.add(Source.single(mockedSourcefile))
        val toTest = builder.add(new FileExistsSwitch(mockedStorageRef))
        val ign = builder.add(Sink.ignore)

        src ~> toTest ~> sink
        toTest.out(1) ~> ign

        ClosedShape
      }

      val result = Await.result(RunnableGraph.fromGraph(graph).run(), 10 seconds)

      there was one(mockedStorageRef).fullPathFor("/path/to/file")
      there was one(mockedStorageDriver).pathExists("/full/path/to/file",3)
      result.head mustEqual mockedSourcefile
      result.length mustEqual 1
    }

    "push to NO if the storage driver reports that the file does not exist" in new AkkaTestkitSpecs2Support {
      implicit val mat:Materializer = ActorMaterializer.create(system)

      val mockedStorageDriver = mock[StorageDriver]
      mockedStorageDriver.pathExists(any,any) returns false
      val mockedStorageRef = mock[StorageEntry]
      mockedStorageRef.getStorageDriver returns Some(mockedStorageDriver)
      mockedStorageRef.fullPathFor(any) returns "/full/path/to/file"

      val sinkFact = Sink.fold[Seq[FileEntry],FileEntry](Seq())((acc,elem)=>acc:+elem)

      val mockedSourcefile = makeFileEntry()
      val graph = GraphDSL.create(sinkFact) { implicit builder=> sink=>
        import akka.stream.scaladsl.GraphDSL.Implicits._

        val src = builder.add(Source.single(mockedSourcefile))
        val toTest = builder.add(new FileExistsSwitch(mockedStorageRef))
        val ign = builder.add(Sink.ignore)

        src ~> toTest ~> ign
        toTest.out(1) ~> sink

        ClosedShape
      }

      val result = Await.result(RunnableGraph.fromGraph(graph).run(), 10 seconds)

      there was one(mockedStorageRef).fullPathFor("/path/to/file")
      there was one(mockedStorageDriver).pathExists("/full/path/to/file",3)
      result.head mustEqual mockedSourcefile
      result.length mustEqual 1
    }

    "raise an exception if there is no driver associated with the given storage" in new AkkaTestkitSpecs2Support {
      implicit val mat:Materializer = ActorMaterializer.create(system)

      val mockedStorageDriver = mock[StorageDriver]
      mockedStorageDriver.pathExists(any,any) returns true
      val mockedStorageRef = mock[StorageEntry]
      mockedStorageRef.getStorageDriver returns None
      mockedStorageRef.fullPathFor(any) returns "/full/path/to/file"

      val sinkFact = Sink.fold[Seq[FileEntry],FileEntry](Seq())((acc,elem)=>acc:+elem)

      val mockedSourcefile = makeFileEntry()
      val graph = GraphDSL.create(sinkFact) { implicit builder=> sink=>
        import akka.stream.scaladsl.GraphDSL.Implicits._

        val src = builder.add(Source.single(mockedSourcefile))
        val toTest = builder.add(new FileExistsSwitch(mockedStorageRef))
        val ign = builder.add(Sink.ignore)

        src ~> toTest ~> sink
        toTest.out(1) ~> ign

        ClosedShape
      }

      def theTest = {
        Await.result(RunnableGraph.fromGraph(graph).run(), 10 seconds)
      }

      theTest must throwA[RuntimeException]

      there was no(mockedStorageRef).fullPathFor(any)
      there was no(mockedStorageDriver).pathExists(any,any)

    }
  }
}
