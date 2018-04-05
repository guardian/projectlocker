package models

import java.util.UUID

import play.api.Logger
import play.api.libs.functional.syntax.unlift
import play.api.libs.json._
import play.api.libs.functional.syntax._
import slick.lifted.{TableQuery, Tag}
import slick.jdbc.PostgresProfile.api._
import slick.sql.SqlProfile.ColumnOption.SqlType

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

case class IncomingProjectSubtype(name:String, uuid:String, parent_name:String) {
  def toPlutoProjectType(implicit db: slick.jdbc.PostgresProfile#Backend#Database):Future[Option[PlutoProjectType]] = {
    PlutoProjectType.entryForName(parent_name).map({
      case Some(parentEntry)=> Some(PlutoProjectType(None, name, uuid, parentEntry.id, None))
      case None=>None
    })
  }
}

case class PlutoProjectType(id: Option[Int], name: String, uuid: String, parent:Option[Int], defaultProjectTemplate:Option[Int]) {
  val logger = Logger(getClass)
  /**
    *  writes this model into the database, inserting if id is None and returning a fresh object with id set. If an id
    * was set, then updates the database record and returns the same object. */
  def save(implicit db: slick.jdbc.PostgresProfile#Backend#Database):Future[Try[PlutoProjectType]] = id match {
    case None=>
      val insertQuery = TableQuery[PlutoProjectTypeRow] returning TableQuery[PlutoProjectTypeRow].map(_.id) into ((item,id)=>item.copy(id=Some(id)))
      db.run(
        (insertQuery+=this).asTry
      ).map({
        case Success(insertResult)=>Success(insertResult.asInstanceOf[PlutoProjectType])  //maybe only intellij needs the cast here?
        case Failure(error)=>Failure(error)
      })
    case Some(realEntityId)=>
      db.run(
        TableQuery[PlutoProjectTypeRow].filter(_.id===realEntityId).update(this).asTry
      ).map({
        case Success(rowsAffected)=>Success(this)
        case Failure(error)=>Failure(error)
      })
  }

  /**
    * inserts this record into the database if there is nothing with the given uuid present
    * @param db - implicitly provided database object
    * @return a Future containing a Try containing a [[PlutoWorkingGroup]] object.
    *         If it was newly saved, or exists in the db, the id member will be set.
    */
  def ensureRecorded(implicit db: slick.jdbc.PostgresProfile#Backend#Database):Future[Try[PlutoProjectType]] = {
    db.run(
      TableQuery[PlutoProjectTypeRow].filter(_.uuid===uuid).result.asTry
    ).flatMap({
      case Success(rows)=>
        if(rows.isEmpty) {
          logger.info(s"Saving working group $name ($uuid) to the database")
          this.save
        } else {
          Future(Success(rows.head))
        }
      case Failure(error)=>
        throw error
        Future(Failure(error))
    })
  }
}

object PlutoProjectType extends ((Option[Int], String, String, Option[Int], Option[Int])=>PlutoProjectType) {
  def entryForName(name:String)(implicit db: slick.jdbc.PostgresProfile#Backend#Database):Future[Option[PlutoProjectType]] =
    db.run(
      TableQuery[PlutoProjectTypeRow].filter(_.name===name).result.asTry
    ).map({
      case Success(resultSeq)=>resultSeq.headOption
      case Failure(error)=>throw error
    })

  def entryForUuid(uuid:String)(implicit db: slick.jdbc.PostgresProfile#Backend#Database):Future[Option[PlutoProjectType]] = entryForUuid(UUID.fromString(uuid))

  def entryForUuid(uuid: UUID)(implicit db: slick.jdbc.PostgresProfile#Backend#Database):Future[Option[PlutoProjectType]] =
    db.run(
      TableQuery[PlutoProjectTypeRow].filter(_.uuid===uuid.toString).result
    ).map(_.headOption)

  def entryForProjectLockerTemplate(templateId:Int)(implicit db: slick.jdbc.PostgresProfile#Backend#Database):Future[Seq[PlutoProjectType]] =
    db.run(
      TableQuery[PlutoProjectTypeRow].filter(_.defaultProjectTemplate===templateId).result
    )
}

class PlutoProjectTypeRow(tag:Tag) extends Table[PlutoProjectType](tag, "PlutoProjectType") {
  def id = column[Int]("id",O.PrimaryKey, O.AutoInc)
  def name = column[String]("s_name")
  def uuid = column[String]("u_uuid")
  def parent = column[Int]("k_parent")
  def defaultProjectTemplate = column[Int]("k_default_template")

  def * = (id.?, name, uuid, parent.?, defaultProjectTemplate.?) <> (PlutoProjectType.tupled, PlutoProjectType.unapply)
}

trait PlutoProjectTypeSerializer {
  implicit val plutoProjectTypeReads:Reads[PlutoProjectType] = (
    (JsPath \ "id").readNullable[Int] and
    (JsPath \ "name").read[String] and
      (JsPath \ "uuid").read[String] and
      (JsPath \ "parent").readNullable[Int] and
      (JsPath \ "defaultProjectTemplate").readNullable[Int]
  )(PlutoProjectType.apply _)

  implicit val plutoProjectTypeWrites:Writes[PlutoProjectType] = (
    (JsPath \ "id").writeNullable[Int] and
      (JsPath \ "name").write[String] and
      (JsPath \ "uuid").write[String] and
      (JsPath \ "parent").writeNullable[Int] and
      (JsPath \ "defaultProjectType").writeNullable[Int]
    )(unlift(PlutoProjectType.unapply))

  implicit val plutoProjectSubtypeReads:Reads[IncomingProjectSubtype] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "uuid").read[String] and
      (JsPath \ "parent_type").read[String]
  )(IncomingProjectSubtype.apply _)
}