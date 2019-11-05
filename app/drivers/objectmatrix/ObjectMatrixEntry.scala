package drivers.objectmatrix

import java.time.{Instant, ZoneId, ZonedDateTime}

import akka.stream.Materializer
import com.om.mxs.client.japi.Vault

import scala.concurrent.ExecutionContext

object ObjectMatrixEntry extends ((String,Option[MxsMetadata],Option[FileAttributes])=>ObjectMatrixEntry ){
  def apply(oid:String) = new ObjectMatrixEntry(oid, None, None)

}

case class ObjectMatrixEntry(oid:String, attributes:Option[MxsMetadata], fileAttribues:Option[FileAttributes]) {
  def getMxsObject(implicit vault:Vault) = vault.getObject(oid)

  def getMetadata(implicit vault:Vault, mat:Materializer, ec:ExecutionContext) = MetadataHelper
    .getAttributeMetadata(getMxsObject)
    .map(mxsMeta=>
      this.copy(oid, Some(mxsMeta), Some(FileAttributes(MetadataHelper.getMxfsMetadata(getMxsObject))))
    )

  def hasMetadata:Boolean = attributes.isDefined && fileAttribues.isDefined

  def stringAttribute(key:String) = attributes.flatMap(_.stringValues.get(key))
  def intAttribute(key:String) = attributes.flatMap(_.intValues.get(key))
  def longAttribute(key:String) = attributes.flatMap(_.longValues.get(key))
  def timeAttribute(key:String, zoneId:ZoneId=ZoneId.systemDefault()) = attributes
    .flatMap(_.longValues.get(key))
    .map(v=>ZonedDateTime.ofInstant(Instant.ofEpochMilli(v),zoneId))

  def maybeGetPath() = stringAttribute("MXFS_PATH")
  def maybeGetFilename() = stringAttribute("MXFS_FILENAME")

  def pathOrFilename = maybeGetPath() match {
    case Some(p)=>Some(p)
    case None=>maybeGetFilename()
  }
}