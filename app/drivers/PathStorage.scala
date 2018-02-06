package drivers

import java.io.{BufferedInputStream, DataOutputStream, File, FileOutputStream}
import java.util.NoSuchElementException

/**
  * Implements a storage driver for regular file paths
  */
class PathStorage extends StorageDriver{
  override def fileForPath(path: String) = {
    new File(path)
  }

//  override def writeDataToPath(path:String, dataStream:Stream[Byte]) = {
//    val f = this.fileForPath(path)
//    val st = new FileOutputStream(f)
//
//    def writeStreamChunk(st:FileOutputStream, dataStream:Stream[Byte]):Boolean = {
//      st.write(dataStream.head)
//      writeStreamChunk(st, dataStream.tail)
//    }
//    writeStreamChunk(st, dataStream)
//    st.close()
//  }

  override def writeDataToPath(path: String, dataStream: BufferedInputStream): Unit = {
    val f = this.fileForPath(path)
    val st = new FileOutputStream(f)

    def writeStreamChunk(st:FileOutputStream, dataStream:BufferedInputStream):Boolean = {
      if(dataStream.available()<1) return false

      st.write(dataStream.read())
      writeStreamChunk(st,dataStream)
    }

    writeStreamChunk(st, dataStream)
    st.close()
  }

  def writeDataToPath(path:String, data:Array[Byte]) = {
    val f = this.fileForPath(path)
    val st = new FileOutputStream(f)

    st.write(data)
    st.close()
  }
}
