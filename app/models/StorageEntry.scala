package models

import org.joda.time.DateTime
import slick.jdbc.PostgresProfile.api._
import java.sql.Timestamp

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, Writes}

import scala.concurrent.ExecutionContext.Implicits.global
import drivers._
import play.api.Logger
import slick.lifted.TableQuery

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait StorageSerializer {
  /*https://www.playframework.com/documentation/2.5.x/ScalaJson*/
  implicit val storageWrites:Writes[StorageEntry] = (
    (JsPath \ "id").writeNullable[Int] and
      (JsPath \ "rootpath").writeNullable[String] and
      (JsPath \ "clientpath").writeNullable[String] and
      (JsPath \ "storageType").write[String] and
      (JsPath \ "user").writeNullable[String] and
      (JsPath \ "password").writeNullable[String] and
      (JsPath \ "host").writeNullable[String] and
      (JsPath \ "port").writeNullable[Int]
    )(unlift(StorageEntry.unapply))

  implicit val storageReads:Reads[StorageEntry] = (
    (JsPath \ "id").readNullable[Int] and
      (JsPath \ "rootpath").readNullable[String] and
      (JsPath \ "clientpath").readNullable[String] and
      (JsPath \ "storageType").read[String] and
      (JsPath \ "user").readNullable[String] and
      (JsPath \ "password").readNullable[String] and
      (JsPath \ "host").readNullable[String] and
      (JsPath \ "port").readNullable[Int]
    )(StorageEntry.apply _)
}

case class StorageEntry(id: Option[Int], rootpath: Option[String], clientpath: Option[String], storageType: String,
                        user:Option[String], password:Option[String], host:Option[String], port:Option[Int]) {
  val logger: Logger = Logger(this.getClass)

  def getStorageDriver:Option[StorageDriver] = {
    if(storageType=="Local"){
      Some(new PathStorage(this))
    } else {
      logger.warn(s"No storage driver defined for $storageType")
      None
    }
  }

  def repr:String = {
    s"$storageType (${rootpath.getOrElse("no root path")}) ${host.getOrElse("(no host)")}"
  }
}

class StorageEntryRow(tag:Tag) extends Table[StorageEntry](tag, "StorageEntry") {
  def id = column[Int]("id",O.PrimaryKey, O.AutoInc)
  def rootpath = column[Option[String]]("s_root_path")
  def clientpath = column[Option[String]]("s_client_path")
  def storageType = column[String]("s_storage_type")
  def user = column[Option[String]]("s_user")
  def password = column[Option[String]]("s_password")
  def host = column[Option[String]]("s_host")
  def port = column[Option[Int]]("i_port")

  def * = (id.?,rootpath,clientpath,storageType,user,password,host,port) <> (StorageEntry.tupled, StorageEntry.unapply)
}


object StorageEntryHelper {
  def entryFor(entryId: Int)(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Option[StorageEntry]] =
    db.run(
      TableQuery[StorageEntryRow].filter(_.id===entryId).result.asTry
    ).map({
      case Success(result)=>
        result.headOption
      case Failure(error)=>throw error
    })
}