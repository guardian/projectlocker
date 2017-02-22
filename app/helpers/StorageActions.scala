package helpers
import java.io.{BufferedInputStream, BufferedOutputStream}
import models.StorageEntry
import play.api.Logger

/**
  * Created by localhome on 21/02/2017.
  */
object StorageActions {
  val logger: Logger = Logger(this.getClass)

  def helperFromStorageEntry(storageEntry: StorageEntry): Option[StorageActions] = {
    storageEntry.storageType match {
      case "local"=>
        Some(new LocalStorageActions(storageEntry.id.getOrElse("-1").toString,storageEntry.rootpath.get))
      case _=>
        logger.error(s"Unrecognised storage type for id ${storageEntry.id}: ${storageEntry.storageType}")
        None
    }
  }
}

trait StorageActions {
  val logger: Logger = Logger(this.getClass)

  val storageName:String
  val rootPath:String

  def readFile(fileKey: String): Option[BufferedInputStream]
  def writeFile(fileKey: String, attributes: Map[String,String]): Option[BufferedOutputStream]

}
