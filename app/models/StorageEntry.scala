package models

import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._
import java.sql.Timestamp

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, Writes}

import scala.concurrent.ExecutionContext.Implicits.global
import drivers._
import play.api.Logger

trait StorageSerializer {
  /*https://www.playframework.com/documentation/2.5.x/ScalaJson*/
  implicit val storageWrites:Writes[StorageEntry] = (
    (JsPath \ "id").writeNullable[Int] and
      (JsPath \ "rootpath").writeNullable[String] and
      (JsPath \ "storageType").write[String] and
      (JsPath \ "user").writeNullable[String] and
      (JsPath \ "password").writeNullable[String] and
      (JsPath \ "host").writeNullable[String] and
      (JsPath \ "port").writeNullable[Int] and
      (JsPath \ "default").writeNullable[Boolean]
    )(unlift(StorageEntry.unapply))

  implicit val storageReads:Reads[StorageEntry] = (
    (JsPath \ "id").readNullable[Int] and
      (JsPath \ "rootpath").readNullable[String] and
      (JsPath \ "storageType").read[String] and
      (JsPath \ "user").readNullable[String] and
      (JsPath \ "password").readNullable[String] and
      (JsPath \ "host").readNullable[String] and
      (JsPath \ "port").readNullable[Int] and
      (JsPath \ "default").readNullable[Boolean]
    )(StorageEntry.apply _)
}

case class StorageEntry(id: Option[Int], rootpath: Option[String], storageType: String,
                        user:Option[String], password:Option[String], host:Option[String], port:Option[Int], default:Option[Boolean]) {

  def getStorageDriver:Option[StorageDriver] = {
    if(storageType=="Local"){
      Some(new PathStorage(this))
    } else {
      Logger.warn(s"No storage driver defined for $storageType")
      None
    }
  }
}


class StorageEntryRow(tag:Tag) extends Table[StorageEntry](tag, "StorageEntry") {
  def id = column[Int]("id",O.PrimaryKey, O.AutoInc)
  def rootpath = column[Option[String]]("rootpath")
  def storageType = column[String]("storageType")
  def user = column[Option[String]]("user")
  def password = column[Option[String]]("password")
  def host = column[Option[String]]("host")
  def port = column[Option[Int]]("port")
  def default = column[Option[Boolean]]("default")

  def * = (id.?,rootpath,storageType,user,password,host,port,default) <> (StorageEntry.tupled, StorageEntry.unapply)
}


