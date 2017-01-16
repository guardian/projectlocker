package models
import slick.driver.PostgresDriver.api._

/**
  * Created by localhome on 12/01/2017.
  */
case class ProjectTemplate (id: Option[Int],name: String, projectTypeId: Int, filePath: String, storageId: Int) {

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