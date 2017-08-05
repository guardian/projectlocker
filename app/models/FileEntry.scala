package models

import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._
import java.sql.Timestamp

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads.jodaDateReads
import play.api.libs.json.Writes.jodaDateWrites
import play.api.libs.json._
import slick.driver.JdbcProfile
import slick.jdbc.JdbcBackend

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import scala.concurrent.{Await, Future}

case class FileEntry(id: Option[Int], filepath: String, storageId: Int, user:String,version:Int,
                     ctime: Timestamp, mtime: Timestamp, atime: Timestamp) {

  /* returns a StorageEntry object for the id of the storage of this FileEntry */
  def storage(db: JdbcProfile#Backend#Database):Future[Option[StorageEntry]] = {
    db.run(
      TableQuery[StorageEntryRow].filter(_.id===storageId).result.asTry
    ).map({
      case Success(result)=>Some(result.head)
      case Failure(error)=>None
    })
  }
}

object FileEntry extends ((Option[Int], String, Int, String, Int, Timestamp, Timestamp, Timestamp)=>FileEntry) {
  def entryFor(entryId: Int, db: JdbcBackend.Database):Future[FileEntry] =
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


trait FileEntrySerializer {
  def timestampToDateTime(t: Timestamp): DateTime = new DateTime(t.getTime)
  def dateTimeToTimestamp(dt: DateTime): Timestamp = new Timestamp(dt.getMillis)
  implicit val dateWrites = jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss.SSSZ") //this DOES take numeric timezones - Z means Zone, not literal letter Z
  implicit val dateReads = jodaDateReads("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

  /* performs a conversion from java.sql.Timestamp to Joda DateTime and back again */
  implicit val timestampFormat = new Format[Timestamp] {
    def writes(t: Timestamp): JsValue = Json.toJson(timestampToDateTime(t))
    def reads(json: JsValue): JsResult[Timestamp] = Json.fromJson[DateTime](json).map(dateTimeToTimestamp)
  }

  /*https://www.playframework.com/documentation/2.5.x/ScalaJson*/
  implicit val fileWrites: Writes[FileEntry] = (
    (JsPath \ "id").writeNullable[Int] and
      (JsPath \ "filepath").write[String] and
      (JsPath \ "storage").write[Int] and
      (JsPath \ "user").write[String] and
      (JsPath \ "version").write[Int] and
      (JsPath \ "ctime").write[Timestamp] and
      (JsPath \ "mtime").write[Timestamp] and
      (JsPath \ "atime").write[Timestamp]
    )(unlift(FileEntry.unapply))

  implicit val fileReads: Reads[FileEntry] = (
    (JsPath \ "id").readNullable[Int] and
      (JsPath \ "filepath").read[String] and
      (JsPath \ "storage").read[Int] and
      (JsPath \ "user").read[String] and
      (JsPath \ "version").read[Int] and
      (JsPath \ "ctime").read[Timestamp] and
      (JsPath \ "mtime").read[Timestamp] and
      (JsPath \ "atime").read[Timestamp]
    )(FileEntry.apply _)
}