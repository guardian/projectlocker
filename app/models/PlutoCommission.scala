package models

import java.sql.Timestamp
import java.time.ZonedDateTime

import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.functional.syntax.unlift
import play.api.libs.json._
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
      logger.debug("Inserting commission record")
      val insertQuery = TableQuery[PlutoCommissionRow] returning TableQuery[PlutoCommissionRow].map(_.id) into ((item,id)=>item.copy(id=Some(id)))
      db.run(
        (insertQuery+=this).asTry
      ).map({
        case Success(insertResult)=>
          logger.debug(s"Successful insert: $insertResult")
          Success(insertResult.asInstanceOf[PlutoCommission])  //maybe only intellij needs the cast here?
        case Failure(error)=>
          logger.error(s"could not insert record: ",error)
          Failure(error)
      })
    case Some(realEntityId)=>
      logger.debug(s"Updating commission record $realEntityId")
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
          logger.info(s"Commission $title ($siteId-$collectionId) already existed")
          val updatedInfo = this.copy(id=Some(rows.head.id.get))
          updatedInfo.save
          Future(Success(updatedInfo))
        }
      case Failure(error)=>
        logger.error("could not check commission: ",error)
        Future(Failure(error))
    })
  }

  /**
    * returns the contents as a string->string map, for passing to postrun actions
    * @return
    */
  def asStringMap:Map[String,String] = Map(
    "commissionId"->s"$siteId-$collectionId",
    "commissionCreated"->created.toString,
    "commissionUpdated"->updated.toString,
    "commissionTitle"->title,
    "commissionDescription"->description.getOrElse("")
  )
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

/*
[
    {
        "collection_id": 11,
        "user": 1,
        "created": "2017-12-04T16:11:23.632",
        "updated": "2017-12-04T16:11:28.288",
        "gnm_commission_title": "addasads",
        "gnm_commission_status": "New",
        "gnm_commission_workinggroup": "8b2bc331-7a11-40d0-a1e5-1266bdf8dce5",
        "gnm_commission_description": null,
        "gnm_commission_owner": [
            1
        ]
    },
    {
        "collection_id": 26,
        "user": 1,
        "created": "2017-12-06T15:17:19.425",
        "updated": "2017-12-06T15:17:20.808",
        "gnm_commission_title": "fwqggrgqggreqgr",
        "gnm_commission_status": "New",
        "gnm_commission_workinggroup": "8b2bc331-7a11-40d0-a1e5-1266bdf8dce5",
        "gnm_commission_description": null,
        "gnm_commission_owner": [
            1
        ]
    },
    {
        "collection_id": 13,
        "user": 1,
        "created": "2017-12-04T16:18:12.105",
        "updated": "2018-01-04T14:14:35.346",
        "gnm_commission_title": "addasadsf",
        "gnm_commission_status": "In production",
        "gnm_commission_workinggroup": "8b2bc331-7a11-40d0-a1e5-1266bdf8dce5",
        "gnm_commission_description": null,
        "gnm_commission_owner": [
            1
        ]
    }
]
 */

object PlutoCommission extends ((Option[Int],Int,String,Timestamp,Timestamp,String,String,Option[String],Int)=>PlutoCommission)  {
  def mostRecentByWorkingGroup(workingGroupId: Int)(implicit db: slick.jdbc.JdbcProfile#Backend#Database):Future[Try[Option[PlutoCommission]]] = {
    db.run(
      TableQuery[PlutoCommissionRow].filter(_.workingGroup===workingGroupId).sortBy(_.updated.desc).take(1).result.asTry
    ).map({
      case Failure(error)=>Failure(error)
      case Success(resultList)=> Success(resultList.headOption)
    })
  }

  def entryForVsid(vsid:String)(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Option[PlutoCommission]] = {
    val idparts = vsid.split("-")
    if(idparts.length!=2) return Future(None)

    db.run(
      TableQuery[PlutoCommissionRow].filter(_.siteId===idparts.head).filter(_.collectionId===idparts(1).toInt).result.asTry
    ).map({
      case Success(resultSeq)=>resultSeq.headOption
      case Failure(error)=>throw error
    })
  }

  def entryForId(id:Int)(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Option[PlutoCommission]] = {
    db.run(
      TableQuery[PlutoCommissionRow].filter(_.id===id).result.asTry
    ).map({
      case Success(resultSeq)=>resultSeq.headOption
      case Failure(error)=>throw error
    })
  }

  //handle different explicit time format
  def timestampToDateTime(t: Timestamp): DateTime = new DateTime(t.getTime)
  def dateTimeToTimestamp(dt: DateTime): Timestamp = new Timestamp(dt.getMillis)
  implicit val dateWrites = JodaWrites.jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss.SSS") //this DOES take numeric timezones - Z means Zone, not literal letter Z
  implicit val dateReads = JodaReads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss.SSS")

  /**
    *  performs a conversion from java.sql.Timestamp to Joda DateTime and back again
    */
  implicit val timestampFormat = new Format[Timestamp] {
    def writes(t: Timestamp): JsValue = Json.toJson(timestampToDateTime(t))
    def reads(json: JsValue): JsResult[Timestamp] = Json.fromJson[DateTime](json).map(dateTimeToTimestamp)
  }

  def fromServerRepresentation(serverRep: JsValue, forWorkingGroup: Int, forSiteId: String):Try[PlutoCommission] = {
    try {
      val comm = new PlutoCommission(
        id = None,
        collectionId = (serverRep \ "collection_id").as[Int],
        siteId = forSiteId,
        created = (serverRep \ "created").as[Timestamp],
        updated = (serverRep \ "updated").as[Timestamp],
        title = (serverRep \ "gnm_commission_title").as[String],
        status = (serverRep \ "gnm_commission_status").as[String],
        description = (serverRep \ "gnm_commission_description").asOpt[String],
        workingGroup = forWorkingGroup
      )
      Success(comm)
    } catch {
      case t:Throwable=>Failure(t)
    }
  }
}