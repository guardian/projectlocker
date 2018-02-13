package drivers

import java.io._

import scala.concurrent.Future
import scala.io.Source
import scala.util.Try

/**
  * Protocol for StorageDriver classes
  */
trait StorageDriver {
  val bufferSize = 10
  val storageRef:models.StorageEntry
  def fileForPath(path:String):File

//  def writeDataToPath(path:String, dataStream:Stream[Byte])
  def writeDataToPath(path:String, dataStream:FileInputStream)
  def writeDataToPath(path:String, data:Array[Byte])

  def deleteFileAtPath(path:String):Boolean

  def getReadStream(path:String):Try[InputStream]
  def getWriteStream(path:String):Try[OutputStream]

  def getMetadata(path:String):Map[Symbol,String]
}
