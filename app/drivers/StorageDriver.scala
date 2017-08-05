package drivers

import java.io._

/**
  * Protocol for StorageDriver classes
  */
trait StorageDriver {
  val bufferSize = 10
  def fileForPath(path:String):File

  def writeDataToPath(path:String, dataStream:Stream[Byte])
}
