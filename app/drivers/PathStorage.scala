package drivers

import java.io.{BufferedInputStream, DataOutputStream, File, FileOutputStream}
import java.nio.file.Paths
import java.util.NoSuchElementException

import models.StorageEntry
import play.api.Logger

/**
  * Implements a storage driver for regular file paths
  */
class PathStorage(override val storageRef:StorageEntry) extends StorageDriver{

  override def fileForPath(path: String) = {
    new File(path)
  }

  override def writeDataToPath(path: String, dataStream: BufferedInputStream): Unit = {
    val finalPath = storageRef.rootpath match {
      case Some(rootpath)=>Paths.get(rootpath,path)
      case None=>Paths.get(path)
    }

    val f = this.fileForPath(finalPath.toString)
    Logger.info(s"Writing data to ${f.getAbsolutePath}")
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
    Logger.info(s"Writing data to ${f.getAbsolutePath}")
    val st = new FileOutputStream(f)

    st.write(data)
    st.close()
  }
}
