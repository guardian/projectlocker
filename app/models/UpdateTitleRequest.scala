package models

import play.api.libs.json.{JsPath, Reads, Writes}
import play.api.libs.functional.syntax._

case class UpdateTitleRequest (newTitle:String, newVsid:Option[String])

trait UpdateTitleRequestSerializer {
  implicit val updateTitleRequestReads:Reads[UpdateTitleRequest] = (
    (JsPath \ "title").read[String] and
      (JsPath \ "vsid").readNullable[String]
    )(UpdateTitleRequest.apply _)

}
