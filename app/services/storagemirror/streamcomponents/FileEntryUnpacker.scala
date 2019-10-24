package services.storagemirror.streamcomponents

import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage.{AbstractInHandler, AbstractOutHandler, GraphStage, GraphStageLogic}
import models.FileEntry
import org.slf4j.LoggerFactory

/**
  * unpacks all FileEntry instances from ReplicaJob instances and yields them one at a time
  */
class FileEntryUnpacker extends GraphStage[FlowShape[ReplicaJob, FileEntry]]{
  private final val in:Inlet[ReplicaJob] = Inlet.create("FileEntryUnpacker.in")
  private final val out:Outlet[FileEntry] = Outlet.create("FileEntryUnpacker.out")

  override def shape: FlowShape[ReplicaJob, FileEntry] = FlowShape.of(in,out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    private val logger = LoggerFactory.getLogger(getClass)

    private var outputQueue:Seq[FileEntry] = Seq()

    setHandler(in, new AbstractInHandler {
      override def onPush(): Unit = {
        val incomingJob = grab(in)
        outputQueue ++= Seq(incomingJob.sourceEntry, incomingJob.destEntry)

        push(out,outputQueue.head)
        outputQueue = outputQueue.tail
      }
    })

    setHandler(out, new AbstractOutHandler {
      override def onPull(): Unit = {
        if(outputQueue.nonEmpty){
          push(out, outputQueue.head)
          outputQueue = outputQueue.tail
        } else {
          pull(in)
        }
      }
    })
  }
}
