package models
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads}

case class PlutoProjectUpdateRequest(title:String,productionOffice:String,deletable:Boolean,deepArchive:Boolean,sensitive:Boolean)

trait PlutoUpdateRequestSerializer {
  implicit val plutoUpdateRequestReads:Reads[PlutoProjectUpdateRequest] = (
    (JsPath \ "title").read[String] and
      (JsPath \ "productionOffice").read[String] and
      (JsPath \ "deletable").read[Boolean] and
      (JsPath \ "deepArchive").read[Boolean] and
      (JsPath \ "sensitive").read[Boolean]
  )(PlutoProjectUpdateRequest.apply _)
}