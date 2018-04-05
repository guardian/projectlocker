package models
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, Writes}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.JdbcBackend
import slick.lifted.TableQuery

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

trait ProjectTemplateSerializer {
  /*https://www.playframework.com/documentation/2.5.x/ScalaJson*/
  implicit val templateWrites:Writes[ProjectTemplate] = (
    (JsPath \ "id").writeNullable[Int] and
      (JsPath \ "name").write[String] and
      (JsPath \ "projectTypeId").write[Int] and
      (JsPath \ "fileRef").write[Int] and
      (JsPath \ "plutoSubtype").writeNullable[Int]
    )(unlift(ProjectTemplate.unapply))

  implicit val templateReads:Reads[ProjectTemplate] = (
    (JsPath \ "id").readNullable[Int] and
      (JsPath \ "name").read[String] and
      (JsPath \ "projectTypeId").read[Int] and
      (JsPath \ "fileRef").read[Int] and
      (JsPath \ "plutoSubtype").readNullable[Int]
    )(ProjectTemplate.apply _)
}

case class ProjectTemplate (id: Option[Int],name: String, projectTypeId: Int, fileRef: Int, plutoSubtypeRef: Option[Int]) {
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

  def plutoSubtype(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Option[PlutoProjectType]] = plutoSubtypeRef match {
    case Some(plutoTypeId)=>
      db.run(TableQuery[PlutoProjectTypeRow].filter(_.id===plutoTypeId).result).map(_.headOption)
    case None=>Future(None)
  }

}

class ProjectTemplateRow(tag: Tag) extends Table[ProjectTemplate](tag,"ProjectTemplate") {
  def id=column[Int]("id",O.PrimaryKey,O.AutoInc)
  def name=column[String]("s_name")
  def projectType=column[Int]("k_project_type")
  def fileRef=column[Int]("k_file_ref")
  def plutoSubtype=column[Option[Int]]("k_pluto_subtype")
  def fkProjectType=foreignKey("fk_project_type",projectType,TableQuery[ProjectTypeRow])(_.id)
  def fkFileRef=foreignKey("fk_file_ref",fileRef,TableQuery[FileEntryRow])(_.id)
  def fkPlutoSubtype=foreignKey("fk_pluto_subtype", plutoSubtype,TableQuery[PlutoProjectTypeRow])(_.id)

  def * = (id.?, name, projectType, fileRef, plutoSubtype) <> (ProjectTemplate.tupled, ProjectTemplate.unapply)
}

object ProjectTemplate extends ((Option[Int],String,Int,Int,Option[Int])=>ProjectTemplate) {
  def entryFor(entryId: Int)(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Option[ProjectTemplate]] =
    db.run(
      TableQuery[ProjectTemplateRow].filter(_.id===entryId).result.asTry
    ).map({
      case Success(result)=>result.headOption
      case Failure(error)=>throw error
    })

  def defaultEntryFor(plutoProjectTypeUuid:String)(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Either[String, ProjectTemplate]] =
    PlutoProjectType.entryForUuid(plutoProjectTypeUuid).flatMap({
      case Some(plutoProjectType)=>
        plutoProjectType.defaultProjectTemplate match {
          case Some(defaultProjectTemplate)=>
            ProjectTemplate.entryFor (defaultProjectTemplate).map({
              case Some(projectTemplate)=>Right(projectTemplate)
              case None=>Left(s"The default project template for ${plutoProjectType.name} (${plutoProjectType.uuid}) did not exist")
            })
          case None=>
            Future(Left(s"Project type entry ${plutoProjectType.name} (${plutoProjectType.uuid} has no default project template set"))
        }
      case None=>
        Future(Left(s"No record could be found for the pluto project type $plutoProjectTypeUuid"))
    })

  def templatesForFileId(fileId:Int)(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Try[Seq[ProjectTemplate]]] =
    db.run(
      TableQuery[ProjectTemplateRow].filter(_.fileRef===fileId).result.asTry
    )

  def templatesForTypeId(typeID:Int)(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Try[Seq[ProjectTemplate]]] =
    db.run(
      TableQuery[ProjectTemplateRow].filter(_.projectType===typeID).result.asTry
    )
}