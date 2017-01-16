package models

import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._
import java.sql.Timestamp
import scala.concurrent.ExecutionContext.Implicits.global


/**
  * Created by localhome on 12/01/2017.
  */

case class StorageEntry(override val id: Option[Int], rootpath: Option[String], storageType: String,
                        user:Option[String], password:Option[String], host:Option[String], port:Option[Int])  extends GenericModel(id){

}

class StorageEntryRow(tag:Tag) extends Table[StorageEntry](tag, "StorageEntry") {
  def id = column[Int]("id",O.PrimaryKey, O.AutoInc) //Autoincrement generates invalid SQL for Postgres, not sure why
  def rootpath = column[Option[String]]("rootpath")
  def storageType = column[String]("storageType")
  def user = column[Option[String]]("user")
  def password = column[Option[String]]("password")
  def host = column[Option[String]]("host")
  def port = column[Option[Int]]("port")

  def * = (id.?,rootpath,storageType,user,password,host,port) <> (StorageEntry.tupled, StorageEntry.unapply)
}


