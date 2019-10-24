package services.storagemirror.streamcomponents

import java.sql.Timestamp
import java.time.Instant

import akka.stream.scaladsl.{GraphDSL, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, ClosedShape, Materializer}
import models.FileEntry
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import utils.AkkaTestkitSpecs2Support

import scala.concurrent.Await
import scala.concurrent.duration._

class FileEntryMarkLostSpec extends Specification with Mockito {
  "FileEntryMarkLost" should {
    "set the hasContent field to false and set the lost time field to current time" in new AkkaTestkitSpecs2Support {
      implicit val mat:Materializer = ActorMaterializer.create(system)

      val ctime = Timestamp.from(Instant.parse("2019-01-01T00:00:00Z"))
      val mtime = Timestamp.from(Instant.parse("2019-01-02T09:10:11Z"))
      val atime = Timestamp.from(Instant.parse("2019-01-03T10:11:12Z"))

      val initialEntry = FileEntry(Some(1),"/path/to/file",2,"someuser",3,ctime, mtime, atime, hasContent=true,hasLink=true,mirrorParent=None,lostAt=None)

      val sinkFac = Sink.fold[Seq[FileEntry], FileEntry](Seq())((acc,elem)=>acc:+elem)

      val graph = GraphDSL.create(sinkFac) { implicit builder=> sink=>
        import akka.stream.scaladsl.GraphDSL.Implicits._

        val src = builder.add(Source.single(initialEntry))
        val toTest= builder.add(new FileEntryMarkLost {
          override def getNowTime: Timestamp = Timestamp.from(Instant.parse("2019-03-03T04:05:06Z"))
        })

        src ~> toTest ~> sink
        ClosedShape
      }

      val result = Await.result(RunnableGraph.fromGraph(graph).run(), 10 seconds)

      result.length mustEqual 1
      result.head mustEqual
        FileEntry(Some(1),"/path/to/file",2,"someuser",3,ctime, mtime, atime, hasContent=false,hasLink=true,mirrorParent=None,lostAt=Some(Timestamp.from(Instant.parse("2019-03-03T04:05:06Z"))))
    }
  }
}
