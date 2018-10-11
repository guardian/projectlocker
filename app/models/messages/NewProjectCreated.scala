package models.messages

import models._
import play.api.libs.json.{JsPath, Reads, Writes}
import play.api.libs.functional.syntax._

case class NewProjectCreated(projectEntry:ProjectEntry, projectType:ProjectTypeForPluto, commission:PlutoCommission,
                             timestamp:Long, deletable:Boolean, deep_archive:Boolean, sensitive:Boolean) extends QueuedMessage

trait NewProjectCreatedSerializer extends ProjectEntrySerializer with ProjectTypeSerializer with PlutoCommissionSerializer {
  implicit val newProjectCreatedWrites:Writes[NewProjectCreated] = (
    (JsPath \ "projectEntry").write[ProjectEntry] and
      (JsPath \ "projectType").write[ProjectTypeForPluto] and
      (JsPath \ "commission").write[PlutoCommission] and
      (JsPath \ "timestamp").write[Long] and
      (JsPath \ "deletable").write[Boolean] and
      (JsPath \ "deepArchive").write[Boolean] and
      (JsPath \ "sensitive").write[Boolean]
  )(unlift(NewProjectCreated.unapply))

  implicit val newProjectCreatedReads:Reads[NewProjectCreated] = (
    (JsPath \ "projectEntry").read[ProjectEntry] and
      (JsPath \ "projectType").read[ProjectTypeForPluto] and
      (JsPath \ "commission").read[PlutoCommission] and
      (JsPath \ "timestamp").read[Long] and
      (JsPath \ "deletable").read[Boolean] and
      (JsPath \ "deepArchive").read[Boolean] and
      (JsPath \ "sensitive").read[Boolean]
  )(NewProjectCreated.apply _)
}
