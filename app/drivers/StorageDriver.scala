package drivers

import java.io._

/**
  * Protocol for StorageDriver classes
  */
trait StorageDriver {
  def fileForPath(path:String):File

  def writeDataToPath(path:String, dataStream:DataOutputStream)
}
