package models

import slick.jdbc.PostgresProfile.api._
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
                         projectTitle: String, created:Timestamp, user: String, workingGroupId: Option[Int],
                         commissionId: Option[Int], adobe_uuid: Option[String]) {
  def associatedFiles(implicit db:slick.jdbc.PostgresProfile#Backend#Database): Future[Seq[FileEntry]] = {
    db.run(
      TableQuery[FileAssociationRow].filter(_.projectEntry===this.id.get).result.asTry
    ).map({
      case Success(result)=>result.map(assocTuple=>FileEntry.entryFor(assocTuple._2, db))
      case Failure(error)=> throw error
    }).flatMap(Future.sequence(_)).map(_.flatten)
  }

  /**
    * Gets the working group record associated with this project entry
    * @param db implicitly provided database object
    * @return a Future, containing a Try representing whether the db operation succeeded, containing an Option which has the working group, if there is one.
    */
  def getWorkingGroup(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Option[PlutoWorkingGroup]] = {
    workingGroupId match {
      case None=>Future(None)
      case Some(groupId)=>
        db.run(
          TableQuery[PlutoWorkingGroupRow].filter(_.id===groupId).result.asTry
        ).map({
          case Success(matchingEntries)=>matchingEntries.headOption //should only ever be one or zero matches as id is a unique primary key
          case Failure(error)=>throw error
        })
    }
  }

  /**
    * Gets the commission record associated with this project entry
    * @param db implicitly provided database object
    * @return a Future, containing a Try representing whether the db operation succeeded, containing an Option which has the working group, if there is one.
    */
  def getCommission(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Option[PlutoCommission]] = {
    commissionId match {
      case None=>Future(None)
      case Some(commId)=>
        db.run(
          TableQuery[PlutoCommissionRow].filter(_.id===commId).result.asTry
        ).map({
          case Success(matchingEntries)=>matchingEntries.headOption  //should only ever be one or zero matches as id is a unique primary key
          case Failure(error)=>throw error
        })
    }
  }

  def save(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Try[ProjectEntry]] = id match {
    case None=>
      val insertQuery = TableQuery[ProjectEntryRow] returning TableQuery[ProjectEntryRow].map(_.id) into ((item,id)=>item.copy(id=Some(id)))
      db.run(
        (insertQuery+=this).asTry
      ).map({
        case Success(insertResult)=>Success(insertResult)
        case Failure(error)=>Failure(error)
      })
    case Some(realEntityId)=>
      db.run(
        TableQuery[ProjectEntryRow].filter(_.id===realEntityId).update(this).asTry
      ).map({
        case Success(rowsAffected)=>Success(this)
        case Failure(error)=>Failure(error)
      })
  }

  /**
    * returns the contents of this record as a string->string map, for passing to postrun actions
    * @return
    */
  def asStringMap:Map[String,String] = {
    Map(
      "projectId"->id.getOrElse("").toString,
      "vidispineProjectId"->vidispineProjectId.getOrElse(""),
      "projectTitle"->projectTitle,
      "projectCreated"->created.toString,
      "projectOwner"->user,
      "projectAdobeUuid"->adobe_uuid.getOrElse("")
    )
  }
}

class ProjectEntryRow(tag:Tag) extends Table[ProjectEntry](tag, "ProjectEntry") {
  implicit val DateTimeTotimestamp =
    MappedColumnType.base[DateTime, Timestamp]({d=>new Timestamp(d.getMillis)}, {t=>new DateTime(t.getTime, UTC)})

  def id=column[Int]("id",O.PrimaryKey,O.AutoInc)
  def projectType=column[Int]("k_project_type")
  def vidispineProjectId=column[Option[String]]("s_vidispine_id")
  def projectTitle=column[String]("s_title")
  def created=column[Timestamp]("t_created")
  def user=column[String]("s_user")
  def workingGroup=column[Option[Int]]("k_working_group")
  def commission=column[Option[Int]]("k_commission")
  def adobe_uuid=column[Option[String]]("s_adobe_uuid")

  def projectTypeKey=foreignKey("fk_project_type",projectType,TableQuery[ProjectTypeRow])(_.id)
  def * = (id.?, projectType, vidispineProjectId, projectTitle, created, user, workingGroup, commission, adobe_uuid) <> (ProjectEntry.tupled, ProjectEntry.unapply)
}

trait ProjectEntrySerializer extends TimestampSerialization {
  /*https://www.playframework.com/documentation/2.5.x/ScalaJson*/
  implicit val templateWrites:Writes[ProjectEntry] = (
    (JsPath \ "id").writeNullable[Int] and
      (JsPath \ "projectTypeId").write[Int] and
      (JsPath \ "vidispineId").writeNullable[String] and
      (JsPath \ "title").write[String] and
      (JsPath \ "created").write[Timestamp] and
      (JsPath \ "user").write[String] and
      (JsPath \ "workingGroupId").writeNullable[Int] and
      (JsPath \ "commissionId").writeNullable[Int] and
      (JsPath \ "adobe_uuid").writeNullable[String]
    )(unlift(ProjectEntry.unapply))

  implicit val templateReads:Reads[ProjectEntry] = (
    (JsPath \ "id").readNullable[Int] and
      (JsPath \ "projectTypeId").read[Int] and
      (JsPath \ "vidispineId").readNullable[String] and
      (JsPath \ "title").read[String] and
      (JsPath \ "created").read[Timestamp] and
      (JsPath \ "user").read[String] and
      (JsPath \ "workingGroupId").readNullable[Int] and
      (JsPath \ "commissionId").readNullable[Int] and
      (JsPath \ "adobe_uuid").readNullable[String]
    )(ProjectEntry.apply _)
}

object ProjectEntry extends ((Option[Int], Int, Option[String], String, Timestamp, String, Option[Int], Option[Int], Option[String])=>ProjectEntry) {
  def createFromFile(sourceFile: FileEntry, projectTemplate: ProjectTemplate, title:String, created:Option[LocalDateTime],
                     user:String, workingGroupId: Option[Int], commissionId: Option[Int])
                    (implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Try[ProjectEntry]] = {
    createFromFile(sourceFile, projectTemplate.projectTypeId, title, created, user, workingGroupId, commissionId)
  }

  def entryForId(requestedId: Int)(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Try[ProjectEntry]] = {
    db.run(
      TableQuery[ProjectEntryRow].filter(_.id===requestedId).result.asTry
    ).map(_.map(_.head))
  }

  protected def insertFileAssociation(projectEntryId:Int, sourceFileId:Int)(implicit db:slick.jdbc.PostgresProfile#Backend#Database) = db.run(
    (TableQuery[FileAssociationRow]+=(projectEntryId,sourceFileId)).asTry
  )

  private def dateTimeToTimestamp(from: LocalDateTime) = Timestamp.valueOf(from)

  def createFromFile(sourceFile: FileEntry, projectTypeId: Int, title:String, created:Option[LocalDateTime],
                     user:String, workingGroupId: Option[Int], commissionId: Option[Int])
                    (implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Try[ProjectEntry]] = {

    /* step one - create a new project entry */
    println(s"Passed time: $created")
    val entry = ProjectEntry(None, projectTypeId, None, title, dateTimeToTimestamp(created.getOrElse(LocalDateTime.now())),
      user, workingGroupId, commissionId, None)
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
