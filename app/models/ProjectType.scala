package models
import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._
import java.sql.Timestamp

/**
  * Created by localhome on 12/01/2017.
  */
case class ProjectType(id: Option[Int],name:String, opensWith: String, targetVersion: String) {

}

class ProjectTypeRow(tag: Tag) extends Table[ProjectType](tag, "ProjectType") {
  def id=column[Int]("id",O.PrimaryKey,O.AutoInc)
  def name=column[String]("name")
  def opensWith=column[String]("opensWith")
  def targetVersion=column[String]("targetVersion")

  def * = (id.?, name, opensWith, targetVersion) <> (ProjectType.tupled, ProjectType.unapply)
}