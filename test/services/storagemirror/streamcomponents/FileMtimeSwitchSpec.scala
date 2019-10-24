package services.storagemirror.streamcomponents

import java.sql.Timestamp
import java.time.Instant

import akka.stream.scaladsl.{GraphDSL, Merge, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, ClosedShape, Materializer}
import drivers.StorageDriver
import models.{FileEntry, StorageEntry}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import utils.AkkaTestkitSpecs2Support

import scala.concurrent.Await
import scala.concurrent.duration._

class FileMtimeSwitchSpec extends Specification with Mockito {
  sequential

  def getInitialEntry = {
    val ctime = Timestamp.from(Instant.parse("2019-01-01T00:00:00Z"))
    val mtime = Timestamp.from(Instant.parse("2019-01-02T09:10:11Z"))
    val atime = Timestamp.from(Instant.parse("2019-01-03T10:11:12Z"))

    FileEntry(Some(1),"/path/to/file",2,"someuser",3,ctime, mtime, atime, hasContent=true,hasLink=true,mirrorParent=None,lostAt=None)
  }

  "FileMtimeSwitch" should {
    "throw an exception if the underlying storage has no driver" in new AkkaTestkitSpecs2Support{
      implicit val mat:Materializer = ActorMaterializer.create(system)

      val mockedStorage = mock[StorageEntry]
      mockedStorage.getStorageDriver (any) returns None
      mockedStorage.repr returns "Test storage mock with no driver"

      val initialEntry = getInitialEntry

      val sinkFact = Sink.fold[Seq[FileEntry],FileEntry](Seq())((acc,entry)=>acc:+entry)

      val graph = GraphDSL.create(sinkFact) { implicit builder => sink =>
        import akka.stream.scaladsl.GraphDSL.Implicits._

        val src = builder.add(Source.single(initialEntry))
        val toTest = builder.add(new FileMtimeSwitch(mockedStorage))

        val sinkMerge = builder.add(Merge[FileEntry](2))

        src ~> toTest ~> sinkMerge ~> sink
        toTest.out(1) ~> sinkMerge //nothing should get passed so just link both to the same output

        ClosedShape
      }

      def theTest = {
        val result = Await.result(RunnableGraph.fromGraph(graph).run(), 10 seconds)
        result.length mustEqual 0
      }
      theTest must throwA[RuntimeException]
    }

    "pass an entry to YES (output 0) if the filesystem modtime is later than the database modtime" in new AkkaTestkitSpecs2Support {
      implicit val mat:Materializer = ActorMaterializer.create(system)

      val mockedStorageDriver = mock[StorageDriver]
      mockedStorageDriver.getMetadata(any,any) returns Map('lastModified->"1571927460")

      val mockedStorage = mock[StorageEntry]
      mockedStorage.getStorageDriver (any) returns Some(mockedStorageDriver)
      mockedStorage.repr returns "Test storage mock with mocked driver"

      val sinkFact = Sink.fold[Seq[FileEntry],FileEntry](Seq())((acc,entry)=>acc:+entry)

      val graph = GraphDSL.create(sinkFact) { implicit builder => sink =>
        import akka.stream.scaladsl.GraphDSL.Implicits._

        val src = builder.add(Source.single(getInitialEntry))
        val toTest = builder.add(new FileMtimeSwitch(mockedStorage))

        val ign = builder.add(Sink.ignore)

        src ~> toTest ~> sink
        toTest.out(1) ~> ign //result can only be passed to one output so safe to ignore the other

        ClosedShape
      }

      val result = Await.result(RunnableGraph.fromGraph(graph).run(), 10 seconds)
      result.length mustEqual 1
    }

    "pass an entry to NO (output 1) if the filesystem modtime is earlier than the database modtime" in new AkkaTestkitSpecs2Support {
      implicit val mat:Materializer = ActorMaterializer.create(system)

      val mockedStorageDriver = mock[StorageDriver]
      mockedStorageDriver.getMetadata(any,any) returns Map('lastModified->"1546420211")

      val mockedStorage = mock[StorageEntry]
      mockedStorage.getStorageDriver (any) returns Some(mockedStorageDriver)
      mockedStorage.repr returns "Test storage mock with mocked driver"

      val sinkFact = Sink.fold[Seq[FileEntry],FileEntry](Seq())((acc,entry)=>acc:+entry)

      val graph = GraphDSL.create(sinkFact) { implicit builder => sink =>
        import akka.stream.scaladsl.GraphDSL.Implicits._

        val src = builder.add(Source.single(getInitialEntry))
        val toTest = builder.add(new FileMtimeSwitch(mockedStorage))

        val ign = builder.add(Sink.ignore)

        src ~> toTest ~> ign //result can only be passed to one output so safe to ignore the other
        toTest.out(1) ~> sink

        ClosedShape
      }

      val result = Await.result(RunnableGraph.fromGraph(graph).run(), 10 seconds)
      result.length mustEqual 1
    }

    "pass an entry to NO (output 1) if the filesystem modtime in milliseconds is earlier than the database modtime" in new AkkaTestkitSpecs2Support {
      implicit val mat:Materializer = ActorMaterializer.create(system)

      val mockedStorageDriver = mock[StorageDriver]
      mockedStorageDriver.getMetadata(any,any) returns Map('lastModified->"1546420211000")

      val mockedStorage = mock[StorageEntry]
      mockedStorage.getStorageDriver (any) returns Some(mockedStorageDriver)
      mockedStorage.repr returns "Test storage mock with mocked driver"

      val sinkFact = Sink.fold[Seq[FileEntry],FileEntry](Seq())((acc,entry)=>acc:+entry)

      val graph = GraphDSL.create(sinkFact) { implicit builder => sink =>
        import akka.stream.scaladsl.GraphDSL.Implicits._

        val src = builder.add(Source.single(getInitialEntry))
        val toTest = builder.add(new FileMtimeSwitch(mockedStorage))

        val ign = builder.add(Sink.ignore)

        src ~> toTest ~> ign //result can only be passed to one output so safe to ignore the other
        toTest.out(1) ~> sink

        ClosedShape
      }

      val result = Await.result(RunnableGraph.fromGraph(graph).run(), 10 seconds)
      result.length mustEqual 1
    }


  }
}
