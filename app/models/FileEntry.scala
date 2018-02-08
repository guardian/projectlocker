package models

import java.nio.file.{Path, Paths}

import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._
import java.sql.Timestamp

import play.api.Logger
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads.jodaDateReads
import play.api.libs.json.Writes.jodaDateWrites
import play.api.libs.json._
import slick.jdbc.JdbcBackend

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import scala.concurrent.{Await, Future}

case class FileEntry(id: Option[Int], filepath: String, storageId: Int, user:String,version:Int,
                     ctime: Timestamp, mtime: Timestamp, atime: Timestamp, hasContent:Boolean, hasLink:Boolean) {

  /* returns a StorageEntry object for the id of the storage of this FileEntry */
  def storage(implicit db: slick.driver.JdbcProfile#Backend#Database):Future[Option[StorageEntry]] = {
    db.run(
      TableQuery[StorageEntryRow].filter(_.id===storageId).result.asTry
    ).map({
      case Success(result)=>Some(result.head)
      case Failure(error)=>None
    })
  }

  def getFullPath(implicit db: slick.driver.JdbcProfile#Backend#Database):Future[String] = {
    this.storage.map({
      case Some(storage)=>
        Paths.get(storage.rootpath.getOrElse(""), filepath).toString
      case None=>
        filepath
    })
  }

  def deleteFromDisk(implicit db:slick.driver.JdbcProfile#Backend#Database):Future[Either[String,Boolean]] = {
    /*this attempts to delete the file from disk, using the configured storage driver*/
    /*it either returns a Right(), with a boolean indicating whether the delete happened or not, or a Left() with an error string*/
    val maybeStorageDriverFuture = this.storage.map({
      case Some(storage)=>
        storage.getStorageDriver
      case None=>
        None
    })

    maybeStorageDriverFuture.flatMap({
      case Some(storagedriver)=>
        this.getFullPath.map(fullpath=>Right(storagedriver.deleteFileAtPath(fullpath)))
      case None=>
        Future(Left("No storage driver configured for storage"))
    })
  }

  def deleteSelf(implicit db:slick.driver.JdbcProfile#Backend#Database):Future[Either[Throwable, Unit]] = {
    /* attempt to delete the underlying record from the database */
    id match {
      case Some(databaseId)=>
        Logger.info(s"Deleting database record for file $databaseId ($filepath on storage $storageId)")
        db.run(
          TableQuery[FileEntryRow].filter(_.id===databaseId).delete.asTry
        ).map({
          case Success(rowsAffected)=>Right()
          case Failure(error)=>Left(error)
        })
      case None=>
        Future(Left(new RuntimeException("Cannot delete a record that has not been saved to the database")))
    }
  }
}

object FileEntry extends ((Option[Int], String, Int, String, Int, Timestamp, Timestamp, Timestamp, Boolean, Boolean)=>FileEntry) {
  def entryFor(entryId: Int, db:slick.driver.JdbcProfile#Backend#Database):Future[Option[FileEntry]] =
    db.run(
      TableQuery[FileEntryRow].filter(_.id===entryId).result.asTry
    ).map({
      case Success(result)=>
        if(result.isEmpty) {
          None
        } else {
          Some(result.head)
        }
      case Failure(error)=>throw error
    })
}

class FileEntryRow(tag:Tag) extends Table[FileEntry](tag, "FileEntry") {
  def id = column[Int]("id",O.PrimaryKey, O.AutoInc)
  def filepath = column[String]("filepath")
  def storage = column[Int]("storage")
  def version = column[Int]("version")
  def user = column[String]("user")
  def ctime = column[Timestamp]("ctime")
  def mtime = column[Timestamp]("mtime")
  def atime = column[Timestamp]("atime")

  def hasContent = column[Boolean]("has_content")
  def hasLink = column[Boolean]("has_link")

  def storageFk = foreignKey("fk_storage",storage,TableQuery[StorageEntryRow])(_.id)
  def * = (id.?,filepath,storage,user,version,ctime,mtime,atime, hasContent, hasLink) <> (FileEntry.tupled, FileEntry.unapply)
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
      (JsPath \ "atime").write[Timestamp] and
      (JsPath \ "hasContent").write[Boolean] and
      (JsPath \ "hasLink").write[Boolean]
    )(unlift(FileEntry.unapply))

  implicit val fileReads: Reads[FileEntry] = (
    (JsPath \ "id").readNullable[Int] and
      (JsPath \ "filepath").read[String] and
      (JsPath \ "storage").read[Int] and
      (JsPath \ "user").read[String] and
      (JsPath \ "version").read[Int] and
      (JsPath \ "ctime").read[Timestamp] and
      (JsPath \ "mtime").read[Timestamp] and
      (JsPath \ "atime").read[Timestamp] and
      (JsPath \ "hasContent").read[Boolean] and
      (JsPath \ "hasLink").read[Boolean]
    )(FileEntry.apply _)
}