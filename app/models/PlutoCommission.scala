package models

import java.sql.Timestamp

import play.api.Logger
import play.api.libs.functional.syntax.unlift
import play.api.libs.json.{JsPath, Reads, Writes}
import play.api.libs.functional.syntax._
import slick.lifted.{TableQuery, Tag}
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

case class PlutoCommission (id:Option[Int], collectionId:Int, siteId: String, created: Timestamp, updated:Timestamp,
                            title: String, status: String, description: Option[String], workingGroup: Int) {
  private def logger = Logger(getClass)

  /**
    *  writes this model into the database, inserting if id is None and returning a fresh object with id set. If an id
    * was set, then updates the database record and returns the same object. */
  def save(implicit db: slick.jdbc.JdbcProfile#Backend#Database):Future[Try[PlutoCommission]] = id match {
    case None=>
      val insertQuery = TableQuery[PlutoCommissionRow] returning TableQuery[PlutoCommissionRow].map(_.id) into ((item,id)=>item.copy(id=Some(id)))
      db.run(
        (insertQuery+=this).asTry
      ).map({
        case Success(insertResult)=>Success(insertResult.asInstanceOf[PlutoCommission])  //maybe only intellij needs the cast here?
        case Failure(error)=>Failure(error)
      })
    case Some(realEntityId)=>
      db.run(
        TableQuery[PlutoCommissionRow].filter(_.id===realEntityId).update(this).asTry
      ).map({
        case Success(rowsAffected)=>Success(this)
        case Failure(error)=>Failure(error)
      })
  }

  /**
    * inserts this record into the database if there is nothing with the given uuid present
    * @param db - implicitly provided database object
    * @return a Future containing a Try containing a [[PlutoCommission]] object.
    *         If it was newly saved, or exists in the db, the id member will be set.
    */
  def ensureRecorded(implicit db: slick.jdbc.JdbcProfile#Backend#Database):Future[Try[PlutoCommission]] = {
    db.run(
      TableQuery[PlutoCommissionRow].filter(_.collectionId===collectionId).filter(_.siteId===siteId).result.asTry
    ).flatMap({
      case Success(rows)=>
        if(rows.isEmpty) {
          logger.info(s"Saving commission $title ($siteId-$collectionId) to the database")
          this.save
        } else {
          Future(Success(rows.head))
        }
      case Failure(error)=>
        Future(Failure(error))
    })
  }
}

class PlutoCommissionRow (tag:Tag) extends Table[PlutoCommission](tag,"PlutoCommission"){
  def id = column[Int]("id",O.PrimaryKey, O.AutoInc)
  def collectionId = column[Int]("i_collection_id")
  def siteId = column[String]("s_site_id")
  def created = column[Timestamp]("t_created")
  def updated = column[Timestamp]("t_updated")
  def title = column[String]("s_title")
  def status = column[String]("s_status")
  def description = column[Option[String]]("s_description")
  def workingGroup = column[Int]("k_working_group")

  def * = (id.?, collectionId, siteId, created, updated, title, status, description, workingGroup) <> (PlutoCommission.tupled, PlutoCommission.unapply)
}

trait PlutoCommissionSerializer extends TimestampSerialization {
  implicit val plutoCommissionWrites:Writes[PlutoCommission] = (
    (JsPath \ "id").writeNullable[Int] and
      (JsPath \ "collectionId").write[Int] and
      (JsPath \ "siteId").write[String] and
      (JsPath \ "created").write[Timestamp] and
      (JsPath \ "updated").write[Timestamp] and
      (JsPath \ "title").write[String] and
      (JsPath \ "status").write[String] and
      (JsPath \ "description").writeNullable[String] and
      (JsPath \ "workingGroupId").write[Int]
  )(unlift(PlutoCommission.unapply))

  implicit val plutoCommissionReads:Reads[PlutoCommission] = (
    (JsPath \ "id").readNullable[Int] and
      (JsPath \ "collectionId").read[Int] and
      (JsPath \ "siteId").read[String] and
      (JsPath \ "created").read[Timestamp] and
      (JsPath \ "updated").read[Timestamp] and
      (JsPath \ "title").read[String] and
      (JsPath \ "status").read[String] and
      (JsPath \ "description").readNullable[String] and
      (JsPath \ "workingGroupId").read[Int]
    )(PlutoCommission.apply _)
}

object PlutoCommission extends ((Option[Int],Int,String,Timestamp,Timestamp,String,String,Option[String],Int)=>PlutoCommission){
  def mostRecentByWorkingGroup(workingGroupId: Int)(implicit db: slick.jdbc.JdbcProfile#Backend#Database):Future[Try[Option[PlutoCommission]]] = {
    db.run(
      TableQuery[PlutoCommissionRow].filter(_.workingGroup===workingGroupId).sortBy(_.updated.desc).take(1).result.asTry
    ).map({
      case Failure(error)=>Failure(error)
      case Success(resultList)=> Success(resultList.headOption)
    })
  }
}