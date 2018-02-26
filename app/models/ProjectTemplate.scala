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
  def projectType(implicit db: slick.jdbc.JdbcProfile#Backend#Database):Future[ProjectType] = db.run(
    TableQuery[ProjectTypeRow].filter(_.id===projectTypeId).result.asTry
  ).map({
    case Success(result)=>result.head
    case Failure(error)=>throw error
  })

  def file(implicit db:slick.jdbc.JdbcProfile#Backend#Database):Future[FileEntry] = db.run(
    TableQuery[FileEntryRow].filter(_.id===fileRef).result.asTry
  ).map({
    case Success(result)=>result.head
    case Failure(error)=>throw error
  })
}

class ProjectTemplateRow(tag: Tag) extends Table[ProjectTemplate](tag,"ProjectTemplate") {
  def id=column[Int]("id",O.PrimaryKey,O.AutoInc)
  def name=column[String]("name")
  def projectType=column[Int]("ProjectType")
  def fileRef=column[Int]("fileref")

  def fkProjectType=foreignKey("fk_ProjectType",projectType,TableQuery[ProjectTypeRow])(_.id)
  def fkFileRef=foreignKey("fk_FileRef",fileRef,TableQuery[FileEntryRow])(_.id)

  def * = (id.?, name, projectType, fileRef) <> (ProjectTemplate.tupled, ProjectTemplate.unapply)
}

object ProjectTemplateHelper {
  def entryFor(entryId: Int)(implicit db:slick.jdbc.JdbcProfile#Backend#Database):Future[Option[ProjectTemplate]] =
    db.run(
      TableQuery[ProjectTemplateRow].filter(_.id===entryId).result.asTry
    ).map({
      case Success(result)=>
        if(result.isEmpty) {
          None
        } else {
          Some(result.head)
        }
      case Failure(error)=>throw error
    })
}