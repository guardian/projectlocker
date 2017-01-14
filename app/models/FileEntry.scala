package models

import org.joda.time.DateTime
import play.api.db.Database
// Use H2Driver to connect to an H2 database
import slick.driver.H2Driver.api._
import java.sql.Timestamp
import scala.concurrent.ExecutionContext.Implicits.global


/**
  * Created by localhome on 12/01/2017.
  */

case class FileEntry(id: Option[Int], filepath: String, storageType: String, user:String,
                     isDir: Boolean,
                     ctime: Timestamp, mtime: Timestamp, atime: Timestamp) {

}

class FileEntryRow(tag:Tag) extends Table[FileEntry](tag, "FileEntry") {
  def id = column[Int]("id",O.PrimaryKey) //Autoincrement generates invalid SQL for Postgres, not sure why
  def filepath = column[String]("filepath")
  def storageType = column[String]("storageType")
  def user = column[String]("user")
  def isDir = column[Boolean]("isDir")
  def ctime = column[Timestamp]("ctime")
  def mtime = column[Timestamp]("mtime")
  def atime = column[Timestamp]("atime")
  def * = (id.?,filepath,storageType,user,isDir,ctime,mtime,atime) <> (FileEntry.tupled, FileEntry.unapply)
}


