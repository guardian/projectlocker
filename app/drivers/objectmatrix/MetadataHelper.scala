package drivers.objectmatrix

import java.nio.ByteBuffer

import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import com.om.mxs.client.japi.MxsObject
import org.apache.commons.codec.binary.Hex
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.collection.JavaConverters._

object MetadataHelper {
  private val logger = LoggerFactory.getLogger(getClass)
  /**
    * iterates the available metadata and presents it as a dictionary
    * @param obj [[MxsObject]] entity to retrieve information from
    * @param mat implicitly provided materializer for streams
    * @param ec implicitly provided execution context
    * @return a Future, with the relevant map
    */
  def getAttributeMetadata(obj:MxsObject)(implicit mat:Materializer, ec:ExecutionContext) = {
    val view = obj.getAttributeView

    val sink = Sink.fold[MxsMetadata,(String,Any)](MxsMetadata(Map(),Map(),Map(),Map()))((acc,elem)=>{
      elem._2 match {
        case boolValue: Boolean => acc.copy(boolValues = acc.boolValues ++ Map(elem._1->boolValue))
        case intValue:Int => acc.copy(intValues = acc.intValues ++ Map(elem._1 -> intValue))
        case longValue:Long => acc.copy(longValues = acc.longValues ++ Map(elem._1 -> longValue))
        case byteBuffer:ByteBuffer => acc.copy(stringValues = acc.stringValues ++ Map(elem._1 -> Hex.encodeHexString(byteBuffer.array())))
        case stringValue:String => acc.copy(stringValues = acc.stringValues ++ Map(elem._1 -> stringValue))
        case _=>
          logger.warn(s"Could not get metadata value for ${elem._1} on ${obj.getId}, type ${elem._2.getClass.toString} not recognised")
          acc
      }
    })
    Source.fromIterator(()=>view.iterator.asScala)
      .map(elem=>(elem.getKey, elem.getValue))
      .toMat(sink)(Keep.right)
      .run()
  }

  /**
    * get the MXFS file metadata
    * @param obj [[MxsObject]] entity to retrieve information from
    * @return
    */
  def getMxfsMetadata(obj:MxsObject) = {
    val view = obj.getMXFSFileAttributeView
    view.readAttributes()
  }

  def setAttributeMetadata(obj:MxsObject, newMetadata:MxsMetadata) = {
    val view = obj.getAttributeView

    //meh, this is probably not very efficient
    newMetadata.stringValues.foreach(entry=>view.writeString(entry._1,entry._2))
    newMetadata.longValues.foreach(entry=>view.writeLong(entry._1, entry._2))
    newMetadata.intValues.foreach(entry=>view.writeInt(entry._1,entry._2))
    newMetadata.boolValues.foreach(entry=>view.writeBoolean(entry._1, entry._2))
  }
}
