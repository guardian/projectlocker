package models

import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._
import java.sql.Timestamp
import slick.jdbc.JdbcBackend

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import scala.concurrent.{Await, Future}

case class FileEntry(id: Option[Int], filepath: String, storageId: Int, user:String,version:Int,
                     ctime: Timestamp, mtime: Timestamp, atime: Timestamp) {

  /* returns a StorageEntry object for the id of the storage of this FileEntry */
  def storage(db: JdbcBackend.Database):Future[Option[StorageEntry]] = {
    db.run(
      TableQuery[StorageEntryRow].filter(_.id===storageId).result.asTry
    ).map({
      case Success(result)=>Some(result.head)
      case Failure(error)=>None
    })
  }
}

object FileEntry extends ((Option[Int], String, Int, String, Int, Timestamp, Timestamp, Timestamp)=>FileEntry) {
  def entryFor(implicit db: JdbcBackend.Database, entryId: Int):Future[FileEntry] =
    db.run(
      TableQuery[FileEntryRow].filter(_.id===entryId).result.asTry
    ).map({
      case Success(result)=>result.head
      case Failure(error)=>throw error
    })
}

class FileEntryRow(tag:Tag) extends Table[FileEntry](tag, "FileEntry") {
  def id = column[Int]("id",O.PrimaryKey, O.AutoInc) //Autoincrement generates invalid SQL for Postgres, not sure why
  def filepath = column[String]("filepath")
  def storage = column[Int]("storage")
  def version = column[Int]("version")
  def user = column[String]("user")
  def ctime = column[Timestamp]("ctime")
  def mtime = column[Timestamp]("mtime")
  def atime = column[Timestamp]("atime")

  def storageFk = foreignKey("fk_storage",storage,TableQuery[StorageEntryRow])(_.id)
  def * = (id.?,filepath,storage,user,version,ctime,mtime,atime) <> (FileEntry.tupled, FileEntry.unapply)
}


