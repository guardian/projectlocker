package models
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, Writes}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.JdbcBackend
import slick.lifted.TableQuery

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

trait ProjectTemplateSerializer {
  /*https://www.playframework.com/documentation/2.5.x/ScalaJson*/
  implicit val templateWrites:Writes[ProjectTemplate] = (
    (JsPath \ "id").writeNullable[Int] and
      (JsPath \ "name").write[String] and
      (JsPath \ "projectTypeId").write[Int] and
      (JsPath \ "fileRef").write[Int]
    )(unlift(ProjectTemplate.unapply))

  implicit val templateReads:Reads[ProjectTemplate] = (
    (JsPath \ "id").readNullable[Int] and
      (JsPath \ "name").read[String] and
      (JsPath \ "projectTypeId").read[Int] and
      (JsPath \ "fileRef").read[Int]
    )(ProjectTemplate.apply _)
}

case class ProjectTemplate (id: Option[Int],name: String, projectTypeId: Int, fileRef: Int) {
  def projectType(implicit db: slick.jdbc.PostgresProfile#Backend#Database):Future[ProjectType] = db.run(
    TableQuery[ProjectTypeRow].filter(_.id===projectTypeId).result.asTry
  ).map({
    case Success(result)=>result.head
    case Failure(error)=>throw error
  })

  def file(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[FileEntry] = db.run(
    TableQuery[FileEntryRow].filter(_.id===fileRef).result.asTry
  ).map({
    case Success(result)=>result.head
    case Failure(error)=>throw error
  })
}

class ProjectTemplateRow(tag: Tag) extends Table[ProjectTemplate](tag,"ProjectTemplate") {
  def id=column[Int]("id",O.PrimaryKey,O.AutoInc)
  def name=column[String]("s_name")
  def projectType=column[Int]("k_project_type")
  def fileRef=column[Int]("k_file_ref")

  def fkProjectType=foreignKey("fk_project_type",projectType,TableQuery[ProjectTypeRow])(_.id)
  def fkFileRef=foreignKey("fk_file_ref",fileRef,TableQuery[FileEntryRow])(_.id)

  def * = (id.?, name, projectType, fileRef) <> (ProjectTemplate.tupled, ProjectTemplate.unapply)
}

object ProjectTemplate extends ((Option[Int],String,Int,Int)=>ProjectTemplate) {
  def entryFor(entryId: Int)(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Option[ProjectTemplate]] =
    db.run(
      TableQuery[ProjectTemplateRow].filter(_.id===entryId).result.asTry
    ).map({
      case Success(result)=>result.headOption
      case Failure(error)=>throw error
    })

  def defaultEntryFor(plutoProjectType:String)(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Option[ProjectTemplate]] =
    Defaults.entryFor(plutoProjectType).flatMap({
      case Success(maybeEntry)=>
        maybeEntry match {
          case Some(defaultEntry)=>ProjectTemplate.entryFor(defaultEntry.toInt)
          case None=>Future(None)
        }
      case Failure(error)=>throw error
    })
}