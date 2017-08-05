package drivers

import java.io.{DataOutputStream, File}

/**
  * Implements a storage driver for regular file paths
  */
class PathStorage extends StorageDriver{
  override def fileForPath(path: String) = {
    new File(path)
  }

  override def writeDataToPath(path:String, dataStream:DataOutputStream) = {
    
  }
}
