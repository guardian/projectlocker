package models.messages

import models.{ProjectEntry, ProjectEntrySerializer}
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, Writes}

case class NewAdobeUuid (projectEntry:ProjectEntry, newUuid:String) extends QueuedMessage

trait NewAdobeUuidSerializer extends ProjectEntrySerializer{
  implicit val newAdobeUuidFolderWrites:Writes[models.messages.NewAdobeUuid] = (
    (JsPath \ "projectEntry").write[ProjectEntry] and
      (JsPath \ "newUuid").write[String]
    )(unlift(NewAdobeUuid.unapply))

  implicit val newAdobeUuidFolderReads:Reads[models.messages.NewAdobeUuid] = (
    (JsPath \ "projectEntry").read[ProjectEntry] and
      (JsPath \ "newUuid").read[String]
    )(NewAdobeUuid.apply _)
}