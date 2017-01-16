package models
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, Writes}
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcBackend

import scala.concurrent.Future
import scala.util.{Failure, Success}

import scala.concurrent.ExecutionContext.Implicits.global

trait ProjectTemplateSerializer {
  /*https://www.playframework.com/documentation/2.5.x/ScalaJson*/
  implicit val templateWrites:Writes[ProjectTemplate] = (
    (JsPath \ "id").writeNullable[Int] and
      (JsPath \ "name").write[String] and
      (JsPath \ "projectTypeId").write[Int] and
      (JsPath \ "filepath").write[String] and
      (JsPath \ "storageId").write[Int]
    )(unlift(ProjectTemplate.unapply))

  implicit val templateReads:Reads[ProjectTemplate] = (
    (JsPath \ "id").readNullable[Int] and
      (JsPath \ "name").read[String] and
      (JsPath \ "projectTypeId").read[Int] and
      (JsPath \ "filepath").read[String] and
      (JsPath \ "storageId").read[Int]
    )(ProjectTemplate.apply _)
}
case class ProjectTemplate (id: Option[Int],name: String, projectTypeId: Int, filePath: String, storageId: Int) {
  def projectType(implicit db: JdbcBackend.Database):Future[ProjectType] = db.run(
    TableQuery[ProjectTypeRow].filter(_.id===projectTypeId).result.asTry
  ).map({
    case Success(result)=>result.head
    case Failure(error)=>throw error
  })

  def storage(implicit db: JdbcBackend.Database):Future[StorageEntry] = db.run(
    TableQuery[StorageEntryRow].filter(_.id===storageId).result.asTry
  ).map({
    case Success(result)=>result.head
    case Failure(error)=>throw error
  })
}

class ProjectTemplateRow(tag: Tag) extends Table[ProjectTemplate](tag,"ProjectTemplate") {
  def id=column[Int]("id",O.PrimaryKey,O.AutoInc)
  def name=column[String]("name")
  def projectType=column[Int]("ProjectType")
  def filePath=column[String]("filepath")
  def storage=column[Int]("storage")

  def fkProjectType=foreignKey("fk_ProjectType",projectType,TableQuery[ProjectTypeRow])(_.id)
  def fkSourceDir=foreignKey("fk_SourceDir",storage,TableQuery[FileEntryRow])(_.id)

  def * = (id.?, name, projectType, filePath, storage) <> (ProjectTemplate.tupled, ProjectTemplate.unapply)
}