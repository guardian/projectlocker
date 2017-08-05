package helpers
import java.io.{BufferedInputStream, BufferedOutputStream, InputStream, OutputStream}

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

  def copy(input: InputStream, output: OutputStream, chunk: Int = 2048):Int = {
    val buffer = Array.ofDim[Byte](chunk)
    var count  = -1
    var total  = 0

    while({count = input.read(buffer); count > 0}) {
      output.write(buffer, 0, count)
      total+=count
    }
    total
  }
}

trait StorageActions {
  val logger: Logger = Logger(this.getClass)

  val storageName:String
  val rootPath:String

  def readFile(fileKey: String): Option[BufferedInputStream]
  def writeFile(fileKey: String, attributes: Map[String,String]): Option[BufferedOutputStream]

}
