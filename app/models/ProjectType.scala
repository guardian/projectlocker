package models
import slick.jdbc.PostgresProfile.api._
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, Writes}

trait ProjectTypeSerializer {
  /*https://www.playframework.com/documentation/2.5.x/ScalaJson*/
  implicit val templateWrites:Writes[ProjectType] = (
    (JsPath \ "id").writeNullable[Int] and
      (JsPath \ "name").write[String] and
      (JsPath \ "opensWith").write[String] and
      (JsPath \ "targetVersion").write[String] and
      (JsPath \ "fileExtension").writeNullable[String]
    )(unlift(ProjectType.unapply))

  implicit val templateReads:Reads[ProjectType] = (
    (JsPath \ "id").readNullable[Int] and
      (JsPath \ "name").read[String] and
      (JsPath \ "opensWith").read[String] and
      (JsPath \ "targetVersion").read[String] and
      (JsPath \ "fileExtension").readNullable[String]
    )(ProjectType.apply _)
}

case class ProjectType(id: Option[Int],name:String, opensWith: String, targetVersion: String, fileExtension:Option[String]=None) {

}

class ProjectTypeRow(tag: Tag) extends Table[ProjectType](tag, "ProjectType") {
  def id=column[Int]("id",O.PrimaryKey,O.AutoInc)
  def name=column[String]("s_name")
  def opensWith=column[String]("s_opens_with")
  def targetVersion=column[String]("s_target_version")
  def fileExtension=column[Option[String]]("s_file_extension")

  def * = (id.?, name, opensWith, targetVersion, fileExtension) <> (ProjectType.tupled, ProjectType.unapply)
}