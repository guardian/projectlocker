package models

import slick.driver.PostgresDriver.api._
import java.sql.Timestamp

import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import slick.jdbc.JdbcBackend

import scala.concurrent.Future
import scala.util.{Failure, Success}

import scala.concurrent.ExecutionContext.Implicits.global

//object ProjectEntry extends Function5[Int,Int,ProjectType,DateTime,String,ProjectEntry] {
//  def unapply(arg: ProjectEntry): Option[(Option[Int], List[FileEntry], ProjectType, DateTime, String)] = {
//    Some(
//      (
//        arg.id,
//        List(),
//        arg.projectType,
//        arg.created,
//        arg.user
//      )
//    )
//  }
//
//  override def apply(v1: Int, v2: Int, v3: ProjectType, v4: DateTime, v5: String) = {
//    new ProjectEntry(Some(v1), List(), v3, v4, v5)
//  }
//
//  override def apply(v1: Int) = {
//
//  }
//}

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