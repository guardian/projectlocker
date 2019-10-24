package services.storagemirror.streamcomponents

import java.sql.Timestamp
import java.time.{Instant, ZonedDateTime}

import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage.{AbstractInHandler, AbstractOutHandler, GraphStage, GraphStageLogic}
import models.FileEntry
import org.slf4j.LoggerFactory

/**
  * simple flow stage to set hasContent false and a timestamp when the file was noticed lost
  */
class FileEntryMarkLost extends GraphStage[FlowShape[FileEntry,FileEntry]]{
  private val in:Inlet[FileEntry] = Inlet.create("FileEntryMarkLost.in")
  private val out:Outlet[FileEntry] = Outlet.create("FileEntryMarkLost.out")

  override def shape: FlowShape[FileEntry, FileEntry] = FlowShape.of(in, out)

  protected def getNowTime:Timestamp = Timestamp.from(Instant.now())
  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    private val logger = LoggerFactory.getLogger(getClass)

    setHandler(in, new AbstractInHandler {
      override def onPush(): Unit = {
        val incoming = grab(in)
        push(out, incoming.copy(hasContent = false, lostAt = Some(getNowTime)))
      }
    })

    setHandler(out, new AbstractOutHandler {
      override def onPull(): Unit = pull(in)
    })
  }
}
