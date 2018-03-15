package models.messages

import models.{ProjectEntry, ProjectEntrySerializer}
import play.api.libs.json.{JsPath, Reads, Writes}
import play.api.libs.functional.syntax._

case class NewProjectCreated(projectEntry:ProjectEntry, timestamp:Long) extends QueuedMessage

trait NewProjectCreatedSerializer extends ProjectEntrySerializer {
  implicit val newProjectCreatedWrites:Writes[NewProjectCreated] = (
    (JsPath \ "projectEntry").write[ProjectEntry] and
      (JsPath \ "timestamp").write[Long]
  )(unlift(NewProjectCreated.unapply))

  implicit val newProjectCreatedReads:Reads[NewProjectCreated] = (
    (JsPath \ "projectEntry").read[ProjectEntry] and
      (JsPath \ "timestamp").read[Long]
  )(NewProjectCreated.apply _)
}
