package helpers
import java.io._
import java.nio.file.Paths

/**
  * Created by localhome on 22/02/2017.
  */
class LocalStorageActions(override val storageName:String, override val rootPath:String) extends StorageActions {

  override def readFile(fileKey: String) = {
    val filepath = Paths.get(rootPath,fileKey)
    try {
      val stream = new BufferedInputStream(new FileInputStream(filepath.toString))
      Some(stream)
    } catch {
      case e:FileNotFoundException=>
        logger.error("Unable to open file for reading",e)
        None
      case e:SecurityException=>
        logger.error("Unable to open file for reading",e)
        None
    }
  }

  override def writeFile(fileKey: String, attributes: Map[String, String]) = {
    val filepath = Paths.get(rootPath,fileKey)
    try {
      val stream = new BufferedOutputStream(new FileOutputStream(filepath.toString))
      Some(stream)
    } catch {
      case e:FileNotFoundException=>
        logger.error("Unable to open file for writing",e)
        None
      case e:SecurityException=>
        logger.error("Unable to open file for writing",e)
        None
    }
  }
}
