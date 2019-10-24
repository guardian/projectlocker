package services.storagemirror.streamcomponents

import akka.stream.{Attributes, FlowShape, Inlet, Materializer, Outlet}
import akka.stream.stage.{AbstractInHandler, AbstractOutHandler, GraphStage, GraphStageLogic}
import models.StorageEntry
import org.slf4j.LoggerFactory
import org.apache.commons.io.IOUtils

import scala.util.{Failure, Success, Try}

/**
  * copies the file content from a source file to a destination file
  * fills in the ReplicaJob bytesCopied field
  * @param sourceStorage [[StorageEntry]] that contains the source file
  * @param destStorage [[StorageEntry]] that will contain the dest file
  * @param mat implicitly provided ActorMaterializer
  */
class CopyFileContent(sourceStorage:StorageEntry, destStorage:StorageEntry)(implicit mat:Materializer) extends GraphStage[FlowShape[ReplicaJob, ReplicaJob]] {
  private final val in:Inlet[ReplicaJob] = Inlet.create("CopyFileContent.in")
  private final val out:Outlet[ReplicaJob] = Outlet.create("CopyFileContent.out")

  private val maybeSourceStorageDriver = sourceStorage.getStorageDriver
  private val maybeDestStorageDriver = destStorage.getStorageDriver

  override def shape: FlowShape[ReplicaJob, ReplicaJob] = FlowShape.of(in,out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    private val logger = LoggerFactory.getLogger(getClass)

    setHandler(in, new AbstractInHandler {
      override def onPush(): Unit = {
        if(maybeSourceStorageDriver.isEmpty){
          val err = new RuntimeException(s"Source storage has no driver, can't replicate")
          logger.error("Source storage has no driver, can't replicate")
          failStage(err)
          throw err
        }

        if(maybeDestStorageDriver.isEmpty){
          val err = new RuntimeException(s"Destination storage has no driver, can't replicate")
          logger.error("Destination storage has no driver, can't replicate")
          failStage(err)
          throw err
        }

        val incomingJob = grab(in)
        val sourceStorageDriver = maybeSourceStorageDriver.get
        val destStorageDriver = maybeDestStorageDriver.get

        /* compose two Try[*Stream] to a single Try[(InputStream,OutputStream)] */
        val maybeStreams = for {
          sourceStream <- sourceStorageDriver.getReadStream(sourceStorage.fullPathFor(incomingJob.sourceEntry.filepath), incomingJob.sourceEntry.version)
          destStream <- destStorageDriver.getWriteStream(destStorage.fullPathFor(incomingJob.destEntry.filepath), incomingJob.destEntry.version)
        } yield (sourceStream, destStream)

        val outputresult = maybeStreams.flatMap(streams=>Try {
          logger.info(s"Starting copy for $incomingJob")
          IOUtils.copyLarge(streams._1, streams._2)
        })

        outputresult match {
          case Failure(err)=>
            logger.error(s"Could not perform copy for $incomingJob: ", err)
            failStage(err)
            throw err
          case Success(bytesCopied)=>
            logger.info(s"File copy completed for $incomingJob")
            push(out, incomingJob.copy(destEntry=incomingJob.destEntry.copy(hasContent = true),bytesCopied=Some(bytesCopied)))
        }

      }
    })

    setHandler(out, new AbstractOutHandler {
      override def onPull(): Unit = pull(in)
    })
  }
}
