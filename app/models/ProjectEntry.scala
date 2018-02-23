package models

import slick.driver.PostgresDriver.api._
import slick.lifted.TableQuery
import java.sql.Timestamp
import java.time.LocalDateTime

import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads.jodaDateReads
import play.api.libs.json.Writes.jodaDateWrites
import play.api.libs.json._
import slick.jdbc.JdbcBackend

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

case class ProjectEntry (id: Option[Int], projectTypeId: Int, vidispineProjectId: Option[String],
                         projectTitle: String, created:Timestamp, user: String) {
  def associatedFiles(implicit db:slick.driver.JdbcProfile#Backend#Database): Future[Seq[FileEntry]] = {
    db.run(
      TableQuery[FileAssociationRow].filter(_.projectEntry===this.id.get).result.asTry
    ).map({
      case Success(result)=>result.map(assocTuple=>FileEntry.entryFor(assocTuple._2, db))
      case Failure(error)=> throw error
    }).flatMap(Future.sequence(_)).map(_.flatten)
  }

  def save(implicit db:slick.driver.JdbcProfile#Backend#Database):Future[Try[ProjectEntry]] = {
    val insertQuery = TableQuery[ProjectEntryRow] returning TableQuery[ProjectEntryRow].map(_.id) into ((item,id)=>item.copy(id=Some(id)))
    db.run(
      (insertQuery+=this).asTry
    ).map({
      case Success(insertResult)=>Success(insertResult.asInstanceOf[ProjectEntry])  //maybe only intellij needs this?
      case Failure(error)=>Failure(error)
    })
  }
}

class ProjectEntryRow(tag:Tag) extends Table[ProjectEntry](tag, "ProjectEntry") {
  implicit val DateTimeTotimestamp =
    MappedColumnType.base[DateTime, Timestamp]({d=>new Timestamp(d.getMillis)}, {t=>new DateTime(t.getTime, UTC)})

  def id=column[Int]("id",O.PrimaryKey,O.AutoInc)
  def projectType=column[Int]("ProjectType")
  def vidispineProjectId=column[Option[String]]("vidispineId")
  def projectTitle=column[String]("title")
  def created=column[Timestamp]("created")
  def user=column[String]("user")

  def projectTypeKey=foreignKey("ProjectType",projectType,TableQuery[ProjectTypeRow])(_.id)
  def * = (id.?, projectType, vidispineProjectId, projectTitle, created, user) <> (ProjectEntry.tupled, ProjectEntry.unapply)
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
      (JsPath \ "projectTypeId").write[Int] and
      (JsPath \ "vidispineId").writeNullable[String] and
      (JsPath \ "title").write[String] and
      (JsPath \ "created").write[Timestamp] and
      (JsPath \ "user").write[String]
    )(unlift(ProjectEntry.unapply))

  implicit val templateReads:Reads[ProjectEntry] = (
    (JsPath \ "id").readNullable[Int] and
      (JsPath \ "projectTypeId").read[Int] and
      (JsPath \ "vidispineId").readNullable[String] and
      (JsPath \ "title").read[String] and
      (JsPath \ "created").read[Timestamp] and
      (JsPath \ "user").read[String]
    )(ProjectEntry.apply _)
}

object ProjectEntry extends ((Option[Int], Int, Option[String], String, Timestamp, String)=>ProjectEntry) {
  def createFromFile(sourceFile: FileEntry, projectTemplate: ProjectTemplate, title:String, created:Option[LocalDateTime], user:String)
                    (implicit db:slick.driver.JdbcProfile#Backend#Database):Future[Try[ProjectEntry]] = {
    createFromFile(sourceFile, projectTemplate.projectTypeId, title, created, user)
  }

  def entryForId(requestedId: Int)(implicit db:slick.driver.JdbcProfile#Backend#Database):Future[Try[ProjectEntry]] = {
    db.run(
      TableQuery[ProjectEntryRow].filter(_.id===requestedId).result.asTry
    ).map(_.map(_.head))
  }

  protected def insertFileAssociation(projectEntryId:Int, sourceFileId:Int)(implicit db:slick.driver.JdbcProfile#Backend#Database) = db.run(
    (TableQuery[FileAssociationRow]+=(projectEntryId,sourceFileId)).asTry
  )

  private def dateTimeToTimestamp(from: LocalDateTime) = Timestamp.valueOf(from)

  def createFromFile(sourceFile: FileEntry, projectTypeId: Int, title:String, created:Option[LocalDateTime], user:String)
                    (implicit db:slick.driver.JdbcProfile#Backend#Database):Future[Try[ProjectEntry]] = {

    /* step one - create a new project entry */
    println(s"Passed time: $created")
    val entry = ProjectEntry(None, projectTypeId, None, title, dateTimeToTimestamp(created.getOrElse(LocalDateTime.now())), user)
    val savedEntry = entry.save

    /* step two - set up file association. Project entry must be saved, so this is done as a future map */
    savedEntry.flatMap({
      case Success(projectEntry)=>
        if(projectEntry.id.isEmpty){
          Future(Failure(new RuntimeException("Project entry was not saved before setting up file assoication")))
        } else if(sourceFile.id.isEmpty){
          Future(Failure(new RuntimeException("Source file was not saved before setting up file assoication")))
        } else {
          insertFileAssociation(projectEntry.id.get, sourceFile.id.get).map({
            case Success(affectedRows: Int) => Success(projectEntry) //we are not interested in the rows, but the project entry object
            case Failure(error) => Failure(error)
          })
        }
      case Failure(error)=>Future(Failure(error))
    })
  }
}
