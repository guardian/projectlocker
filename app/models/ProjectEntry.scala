package models

import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery
import java.sql.Timestamp
import java.time.LocalDateTime

import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import play.api.libs.functional.syntax._
import play.api.libs.json._
import slick.jdbc.JdbcBackend

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

case class ProjectEntry (id: Option[Int], projectTypeId: Int, vidispineProjectId: Option[String],
                         projectTitle: String, created:Timestamp, user: String, workingGroupId: Option[Int],
                         commissionId: Option[Int]) {
  def associatedFiles(implicit db:slick.jdbc.PostgresProfile#Backend#Database): Future[Seq[FileEntry]] = {
    db.run(
      TableQuery[FileAssociationRow].filter(_.projectEntry===this.id.get).result.asTry
    ).map({
      case Success(result)=>result.map(assocTuple=>FileEntry.entryFor(assocTuple._2, db))
      case Failure(error)=> throw error
    }).flatMap(Future.sequence(_)).map(_.flatten)
  }

  /**
    * get a [[MediaAtomProject]] object for this project
    * @param messageType message type field for media atom
    * @return a Future containing a Try with either the object or a Failure indicating the reason.
    */
  def getMediaAtomMessage(messageType:String):Future[Try[MediaAtomProject]] = commissionId match {
    case Some(realCommissionId)=>
      PlutoCommission.entryFor(realCommissionId).map({
        case Some(plutoCommission)=>
          val commissionId = s"${plutoCommission.siteId}-${plutoCommission.collectionId}"
          Success(MediaAtomProject(messageType, vidispineProjectId.get, projectTitle, "In Production", commissionId, plutoCommission.title, None, created))
        case None=>
          Failure(new RuntimeException(s"No commission existed for project ${this.toString}"))
      }).recover({
        case err:Throwable=>Failure(err)
      })
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

  def removeFromDatabase(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Try[Unit]] = id match {
    case Some(realEntityId)=>
      db.run(DBIO.seq(
        TableQuery[FileAssociationRow].filter(_.projectEntry===realEntityId).delete,
        TableQuery[ProjectEntryRow].filter(_.id===realEntityId).delete,
      ).asTry)
    case None=>
      Future(Failure(new RuntimeException("A record must have been saved before it can be removed from the database")))
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
      "projectOwner"->user
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

  def projectTypeKey=foreignKey("fk_project_type",projectType,TableQuery[ProjectTypeRow])(_.id)
  def * = (id.?, projectType, vidispineProjectId, projectTitle, created, user, workingGroup, commission) <> (ProjectEntry.tupled, ProjectEntry.unapply)
}

trait ProjectEntrySerializer extends TimestampSerialization {
  /*https://www.playframework.com/documentation/2.5.x/ScalaJson*/
  implicit val projectEntryWrites:Writes[ProjectEntry] = (
    (JsPath \ "id").writeNullable[Int] and
      (JsPath \ "projectTypeId").write[Int] and
      (JsPath \ "vidispineId").writeNullable[String] and
      (JsPath \ "title").write[String] and
      (JsPath \ "created").write[Timestamp] and
      (JsPath \ "user").write[String] and
      (JsPath \ "workingGroupId").writeNullable[Int] and
      (JsPath \ "commissionId").writeNullable[Int]
    )(unlift(ProjectEntry.unapply))

  implicit val projectEntryReads:Reads[ProjectEntry] = (
    (JsPath \ "id").readNullable[Int] and
      (JsPath \ "projectTypeId").read[Int] and
      (JsPath \ "vidispineId").readNullable[String] and
      (JsPath \ "title").read[String] and
      (JsPath \ "created").read[Timestamp] and
      (JsPath \ "user").read[String] and
      (JsPath \ "workingGroupId").readNullable[Int] and
      (JsPath \ "commissionId").readNullable[Int]
    )(ProjectEntry.apply _)
}

object ProjectEntry extends ((Option[Int], Int, Option[String], String, Timestamp, String, Option[Int], Option[Int])=>ProjectEntry) {
  def createFromFile(sourceFile: FileEntry, projectTemplate: ProjectTemplate, title:String, created:Option[LocalDateTime],
                     user:String, workingGroupId: Option[Int], commissionId: Option[Int], existingVidispineId: Option[String])
                    (implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Try[ProjectEntry]] = {
    createFromFile(sourceFile, projectTemplate.projectTypeId, title, created, user, workingGroupId, commissionId, existingVidispineId)
  }

  def entryForId(requestedId: Int)(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Try[ProjectEntry]] = {
    db.run(
      TableQuery[ProjectEntryRow].filter(_.id===requestedId).result.asTry
    ).map(_.map(_.head))
  }

  def lookupByVidispineId(vsid: String)(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Try[Seq[ProjectEntry]]] =
    db.run(
      TableQuery[ProjectEntryRow].filter(_.vidispineProjectId===vsid).result.asTry
    )

  /**
    * method to aid recovery if projectlocker has created something but pluto has no record of it. If all parameters match,
    * something is returned
    * @param plutoTypeUuid
    */
  def lookupByPlutoInfo(plutoTypeUuid:String, projectTitle:String, workingGroupUuid:String, commissionId:Int)(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Try[Option[ProjectEntry]]] = {
    val plutoInfoFutures = Future.sequence(Seq(PlutoProjectType.entryForUuid(plutoTypeUuid), PlutoWorkingGroup.entryForUuid(workingGroupUuid)))

    plutoInfoFutures.flatMap(resultSeq=>{
      val maybePlutoProjectType = resultSeq.head.asInstanceOf[Option[PlutoProjectType]]
      val maybePlutoWorkingGroup = resultSeq(1).asInstanceOf[Option[PlutoWorkingGroup]]

      if(maybePlutoProjectType.isDefined && maybePlutoWorkingGroup.isDefined) {
        db.run(
          TableQuery[ProjectEntryRow]
            .filter(_.workingGroup===maybePlutoWorkingGroup.get.id.get)
            .filter(_.projectType===maybePlutoWorkingGroup.get.id.get)
            .filter(_.projectTitle===projectTitle)
            .filter(_.commission===commissionId).result.asTry
        ).map({
          case Success(result)=>Success(result.headOption)
          case Failure(error)=>Failure(error)
        })
      } else {
        Future(Failure(new RuntimeException("Either plutoProjectType or plutoWorkingGroup not found")))
      }
    }).recover({
      case err:Throwable=>Failure(err)
    })
  }

  protected def insertFileAssociation(projectEntryId:Int, sourceFileId:Int)(implicit db:slick.jdbc.PostgresProfile#Backend#Database) = db.run(
    (TableQuery[FileAssociationRow]+=(projectEntryId,sourceFileId)).asTry
  )

  private def dateTimeToTimestamp(from: LocalDateTime) = Timestamp.valueOf(from)

  def createFromFile(sourceFile: FileEntry, projectTypeId: Int, title:String, created:Option[LocalDateTime],
                     user:String, workingGroupId: Option[Int], commissionId: Option[Int], existingVidispineId: Option[String])
                    (implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Try[ProjectEntry]] = {

    /* step one - create a new project entry */
    val entry = ProjectEntry(None, projectTypeId, existingVidispineId, title, dateTimeToTimestamp(created.getOrElse(LocalDateTime.now())),
      user, workingGroupId, commissionId)
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
