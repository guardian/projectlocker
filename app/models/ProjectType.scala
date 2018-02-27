package models
import org.joda.time.DateTime
import slick.jdbc.PostgresProfile.api._
import java.sql.Timestamp

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, Writes}

trait ProjectTypeSerializer {
  /*https://www.playframework.com/documentation/2.5.x/ScalaJson*/
  implicit val templateWrites:Writes[ProjectType] = (
    (JsPath \ "id").writeNullable[Int] and
      (JsPath \ "name").write[String] and
      (JsPath \ "opensWith").write[String] and
      (JsPath \ "targetVersion").write[String]
    )(unlift(ProjectType.unapply))

  implicit val templateReads:Reads[ProjectType] = (
    (JsPath \ "id").readNullable[Int] and
      (JsPath \ "name").read[String] and
      (JsPath \ "opensWith").read[String] and
      (JsPath \ "targetVersion").read[String]
    )(ProjectType.apply _)
}

case class ProjectType(id: Option[Int],name:String, opensWith: String, targetVersion: String) {

}

class ProjectTypeRow(tag: Tag) extends Table[ProjectType](tag, "ProjectType") {
  def id=column[Int]("id",O.PrimaryKey,O.AutoInc)
  def name=column[String]("s_name")
  def opensWith=column[String]("s_opens_with")
  def targetVersion=column[String]("s_target_version")

  def * = (id.?, name, opensWith, targetVersion) <> (ProjectType.tupled, ProjectType.unapply)
}