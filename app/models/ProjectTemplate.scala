package models
import slick.driver.PostgresDriver.api._

/**
  * Created by localhome on 12/01/2017.
  */
case class ProjectTemplate (id: Option[Int],name: String, projectType: ProjectType, sourceDir: FileEntry) {

}

class ProjectTemplateRow(tag: Tag) extends Table[ProjectTemplate](tag,"ProjectTemplate") {
  def id=column[Int]("id",O.PrimaryKey,O.AutoInc)
  def name=column[String]("name")
  def projectType=foreignKey("fk_ProjectType","id",TableQuery[ProjectType])
  def sourceDir=foreignKey("fk_SourceDir","id",TableQuery[FileEntry])

  def * = (id.?, name, projectType, sourceDir) <> (ProjectTemplate.tupled, ProjectTemplate.unapply)
}