package models

import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._
import java.sql.Timestamp
import scala.concurrent.ExecutionContext.Implicits.global

case class FileEntry(id: Option[Int], filepath: String, storage: StorageEntry, user:String,
                     ctime: Timestamp, mtime: Timestamp, atime: Timestamp) {

}

class FileEntryRow(tag:Tag) extends Table[FileEntry](tag, "FileEntry") {
  def id = column[Int]("id",O.PrimaryKey, O.AutoInc) //Autoincrement generates invalid SQL for Postgres, not sure why
  def filepath = column[String]("filepath")
  def storage = foreignKey("fk_storage","id",TableQuery[StorageEntry])
  def user = column[String]("user")
  def ctime = column[Timestamp]("ctime")
  def mtime = column[Timestamp]("mtime")
  def atime = column[Timestamp]("atime")
  def * = (id.?,filepath,storage,user,ctime,mtime,atime) <> (FileEntry.tupled, FileEntry.unapply)
}


