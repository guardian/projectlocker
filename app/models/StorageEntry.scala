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
import scala.util.{Failure, Success, Try}

object StorageStatus extends Enumeration {
  val ONLINE,OFFLINE,DISAPPEARED,MISCONFIGURED,UNKNOWN=Value
}


trait StorageSerializer {
  implicit val storageStatusWrites:Writes[StorageStatus.Value] = Writes.enumNameWrites
  implicit val storageStatusReads:Reads[StorageStatus.Value] = Reads.enumNameReads(StorageStatus)

  /*https://www.playframework.com/documentation/2.5.x/ScalaJson*/
  implicit val storageWrites:Writes[StorageEntry] = (
    (JsPath \ "id").writeNullable[Int] and
      (JsPath \ "rootpath").writeNullable[String] and
      (JsPath \ "clientpath").writeNullable[String] and
      (JsPath \ "storageType").write[String] and
      (JsPath \ "user").writeNullable[String] and
      (JsPath \ "password").writeNullable[String] and
      (JsPath \ "host").writeNullable[String] and
      (JsPath \ "port").writeNullable[Int] and
      (JsPath \ "status").writeNullable[StorageStatus.Value]
    )(unlift(StorageEntry.unapply))

  implicit val storageReads:Reads[StorageEntry] = (
    (JsPath \ "id").readNullable[Int] and
      (JsPath \ "rootpath").readNullable[String] and
      (JsPath \ "clientpath").readNullable[String] and
      (JsPath \ "storageType").read[String] and
      (JsPath \ "user").readNullable[String] and
      (JsPath \ "password").readNullable[String] and
      (JsPath \ "host").readNullable[String] and
      (JsPath \ "port").readNullable[Int] and
      (JsPath \ "status").readNullable[StorageStatus.Value]
    )(StorageEntry.apply _)
}


case class StorageEntry(id: Option[Int], rootpath: Option[String], clientpath: Option[String], storageType: String,
                        user:Option[String], password:Option[String], host:Option[String], port:Option[Int], status:Option[StorageStatus.Value]) {

  def getStorageDriver:Option[StorageDriver] = {
    val logger: Logger = Logger(this.getClass)
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

  def save(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Try[StorageEntry]] = id match {
    case None=>
      val insertQuery = TableQuery[StorageEntryRow] returning TableQuery[StorageEntryRow].map(_.id) into ((item,id)=>item.copy(id=Some(id)))
      db.run(
        (insertQuery+=this).asTry
      )
    case Some(realEntityId)=>
      db.run(
        TableQuery[StorageEntryRow].filter(_.id===realEntityId).update(this).asTry
      ).map({
        case Success(rowsAffected)=>Success(this)
        case Failure(error)=>Failure(error)
      })
  }

  def validatePathExists(filePath:String) = getStorageDriver.map(drv=>drv.pathExists(filePath:String)) match {
    case None=>Left(s"No storage driver exists for storage $id ($rootpath)!")
    case Some(result)=>Right(result)
  }
}

class StorageEntryRow(tag:Tag) extends Table[StorageEntry](tag, "StorageEntry") {
  implicit val storageStatusMapper = MappedColumnType.base[StorageStatus.Value,String](
    e=>e.toString(),
    s=>StorageStatus.withName(s)
  )

  def id = column[Int]("id",O.PrimaryKey, O.AutoInc)
  def rootpath = column[Option[String]]("s_root_path")
  def clientpath = column[Option[String]]("s_client_path")
  def storageType = column[String]("s_storage_type")
  def user = column[Option[String]]("s_user")
  def password = column[Option[String]]("s_password")
  def host = column[Option[String]]("s_host")
  def port = column[Option[Int]]("i_port")
  def status = column[Option[StorageStatus.Value]]("e_status")

  def * = (id.?,rootpath,clientpath,storageType,user,password,host,port,status) <> (StorageEntry.tupled, StorageEntry.unapply)
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

  def defaultProjectfileStorage(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Option[StorageEntry]] =
    Defaults.entryFor("project_storage_id").flatMap({
      case Success(maybeOption)=>
        maybeOption match {
          case Some(storageEntryId)=>StorageEntryHelper.entryFor(storageEntryId.toInt)
          case None=>Future(None)
        }
      case Failure(error)=>throw error
    })

  def allStorages(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Try[Seq[StorageEntry]]] = {
    db.run(
      TableQuery[StorageEntryRow].result.asTry
    )
  }
}