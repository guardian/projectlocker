package drivers

import java.io.{DataOutputStream, File, FileOutputStream}
import java.util.NoSuchElementException

/**
  * Implements a storage driver for regular file paths
  */
class PathStorage extends StorageDriver{
  override def fileForPath(path: String) = {
    new File(path)
  }

  override def writeDataToPath(path:String, dataStream:Stream[Byte]) = {
    val f = this.fileForPath(path)
    val st = new FileOutputStream(f)

    def writeStreamChunk(st:FileOutputStream, dataStream:Stream[Byte]):Boolean = {
      try {
        st.write(dataStream.head)
        writeStreamChunk(st, dataStream.tail)
      } catch {
        case NoSuchElementException=>false
      }
    }
    writeStreamChunk(st, dataStream)
  }
}
