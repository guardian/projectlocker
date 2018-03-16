package models.messages

import models._
import play.api.libs.json.{JsPath, Reads, Writes}
import play.api.libs.functional.syntax._

case class NewProjectCreated(projectEntry:ProjectEntry, projectType:ProjectType, commission:PlutoCommission, timestamp:Long) extends QueuedMessage

trait NewProjectCreatedSerializer extends ProjectEntrySerializer with ProjectTypeSerializer with PlutoCommissionSerializer {
  implicit val newProjectCreatedWrites:Writes[NewProjectCreated] = (
    (JsPath \ "projectEntry").write[ProjectEntry] and
      (JsPath \ "projectType").write[ProjectType] and
      (JsPath \ "commission").write[PlutoCommission] and
      (JsPath \ "timestamp").write[Long]
  )(unlift(NewProjectCreated.unapply))

  implicit val newProjectCreatedReads:Reads[NewProjectCreated] = (
    (JsPath \ "projectEntry").read[ProjectEntry] and
      (JsPath \ "projectType").read[ProjectType] and
      (JsPath \ "commission").read[PlutoCommission] and
      (JsPath \ "timestamp").read[Long]
  )(NewProjectCreated.apply _)
}
