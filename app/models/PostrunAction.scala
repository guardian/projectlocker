package models

import java.sql.Timestamp

import play.api.libs.json.{JsPath, Reads, Writes}
import play.api.libs.functional.syntax._
import slick.lifted.Tag
import slick.jdbc.PostgresProfile.api._

case class PostrunAction (id:Option[Int],runnable:String, title:String, description:Option[String],
                          owner:String, version:Int, ctime: Timestamp){

}

class PostrunActionRow(tag:Tag) extends Table[PostrunAction](tag, "PostrunAction") {
  def id = column[Int]("id",O.PrimaryKey, O.AutoInc)
  def runnable = column[String]("s_runnable")
  def title = column[String]("s_title")
  def description = column[Option[String]]("s_description")
  def owner = column[String]("s_owner")
  def version = column[Int]("i_version")
  def ctime = column[Timestamp]("t_ctime")

  def * = (id.?, runnable, title, description, owner, version, ctime) <> (PostrunAction.tupled, PostrunAction.unapply)
}

trait PostrunActionSerializer extends TimestampSerialization {
  implicit val postrunActionWrites:Writes[PostrunAction] = (
    (JsPath \ "id").writeNullable[Int] and
    (JsPath \ "runnable").write[String] and
      (JsPath \ "title").write[String] and
      (JsPath \ "description").writeNullable[String] and
      (JsPath \ "owner").write[String] and
      (JsPath \ "version").write[Int] and
      (JsPath \ "ctime").write[Timestamp]
  )(unlift(PostrunAction.unapply))

  implicit val postrunActionReads:Reads[PostrunAction] = (
    (JsPath \ "id").readNullable[Int] and
    (JsPath \ "runnable").read[String] and
      (JsPath \ "title").read[String] and
      (JsPath \ "description").readNullable[String] and
      (JsPath \ "owner").read[String] and
      (JsPath \ "version").read[Int] and
      (JsPath \ "ctime").read[Timestamp]
  )(PostrunAction.apply _)
}
