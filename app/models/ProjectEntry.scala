package models

import slick.driver.PostgresDriver.api._
import java.sql.Timestamp

import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads.jodaDateReads
import play.api.libs.json.Writes.jodaDateWrites
import play.api.libs.json._
import slick.jdbc.JdbcBackend

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

case class ProjectEntry (id: Option[Int], fileAssociationId: Int, projectTypeId: Int, created:Timestamp, user: String) {
  def associatedFiles(db: JdbcBackend.Database): Future[Seq[FileEntry]] = {
    db.run(
      TableQuery[FileAssociationRow].filter(_.projectEntry===fileAssociationId).result.asTry
    ).map({
      case Success(result)=>result.map(assocTuple=>FileEntry.entryFor(assocTuple._3, db))
      case Failure(error)=> throw error
    }).flatMap(Future.sequence(_))
  }
}

class ProjectEntryRow(tag:Tag) extends Table[ProjectEntry](tag, "ProjectEntry") {
  implicit val DateTimeTotimestamp =
    MappedColumnType.base[DateTime, Timestamp]({d=>new Timestamp(d.getMillis)}, {t=>new DateTime(t.getTime, UTC)})

  def id=column[Int]("id",O.PrimaryKey,O.AutoInc)
  def fileAssociationId=column[Int]("ProjectFileAssociation")
  def projectType=column[Int]("ProjectType")
  def created=column[Timestamp]("created")
  def user=column[String]("user")

  def fileAssociationKey=foreignKey("fk_ProjectFileAssociation",fileAssociationId,TableQuery[FileAssociationRow])(_.id,onUpdate=ForeignKeyAction.Restrict)
  def projectTypeKey=foreignKey("ProjectType",projectType,TableQuery[ProjectTypeRow])(_.id)
  def * = (id.?, fileAssociationId, projectType, created, user) <> (ProjectEntry.tupled, ProjectEntry.unapply)
}

trait ProjectEntrySerializer {
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
  implicit val templateWrites:Writes[ProjectEntry] = (
    (JsPath \ "id").writeNullable[Int] and
      (JsPath \ "fileAssociationId").write[Int] and
      (JsPath \ "projectTypeId").write[Int] and
      (JsPath \ "created").write[Timestamp] and
      (JsPath \ "user").write[String]
    )(unlift(ProjectEntry.unapply))

  implicit val templateReads:Reads[ProjectEntry] = (
    (JsPath \ "id").readNullable[Int] and
      (JsPath \ "fileAssociationId").read[Int] and
      (JsPath \ "projectTypeId").read[Int] and
      (JsPath \ "created").read[Timestamp] and
      (JsPath \ "user").read[String]
    )(ProjectEntry.apply _)
}