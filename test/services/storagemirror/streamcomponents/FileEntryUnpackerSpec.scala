package services.storagemirror.streamcomponents

import akka.stream.{ActorMaterializer, FlowShape, Materializer}
import akka.stream.scaladsl.{GraphDSL, Keep, Sink, Source}
import models.FileEntry
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import utils.AkkaTestkitSpecs2Support

import scala.concurrent.Await
import scala.concurrent.duration._

class FileEntryUnpackerSpec extends Specification with Mockito {
  sequential

  "FileEntryUnpacker" should {
    "serially emit the FileEntry objects contained in ReplicaJob instances" in new AkkaTestkitSpecs2Support {
      implicit val mat:Materializer = ActorMaterializer.create(system)
      val sourceEntry1 = mock[FileEntry]
      val destEntry1 = mock[FileEntry]
      val sourceEntry2 = mock[FileEntry]
      val destEntry2 = mock[FileEntry]

      val replicaJob1 = ReplicaJob(sourceEntry1,destEntry1,Some(1234))
      val replicaJob2 = ReplicaJob(sourceEntry2,destEntry2,Some(1234))

      val elemToTest = GraphDSL.create() {implicit builder=>
        val toTest = builder.add(new FileEntryUnpacker)

        FlowShape(toTest.in, toTest.out)
      }

      val graph = Source.fromIterator(()=>Array(replicaJob1,replicaJob2).toIterator).via(elemToTest).toMat(Sink.fold[Seq[FileEntry],FileEntry](Seq())((acc,elem)=>acc:+elem))(Keep.right)

      val result = Await.result(graph.run(), 10 seconds)

      result mustEqual Seq(
        sourceEntry1,
        destEntry1,
        sourceEntry2,
        destEntry2
      )
    }
  }
}
