package models

import java.sql.Timestamp

import play.api.libs.functional.syntax._
import play.api.libs.json._

/*
        'type': message_type,
        'id': project_id,
        'title': project_model.gnm_project_headline,
        'status': project_model.gnm_project_status,
        'commissionId': site_id + str(project_model.commission.collection_id),
        'commissionTitle': project_model.commission.gnm_commission_title,
        'productionOffice': None, #this is not used to my knowlege?
        'created': project_model.created.isoformat()

 */

trait MediaAtomProjectSerializer extends TimestampSerialization {
  /*https://www.playframework.com/documentation/2.5.x/ScalaJson*/
  implicit val mediaAtomProjectWrites:Writes[MediaAtomProject] = (
    (JsPath \ "type").write[String] and
    (JsPath \ "id").write[String] and
      (JsPath \ "title").write[String] and
      (JsPath \ "vidispineId").write[String] and
      (JsPath \ "status").write[String] and
      (JsPath \ "commissionId").write[String] and
      (JsPath \ "commissionTitle").write[String] and
      (JsPath \ "productionOffice").writeNullable[String] and
      (JsPath \ "created").writeNullable[Timestamp]
    )(unlift(MediaAtomProject.unapply))

  implicit val mediaAtomProjectReads:Reads[MediaAtomProject] = (
    (JsPath \ "type").read[String] and
      (JsPath \ "id").read[String] and
      (JsPath \ "title").read[String] and
      (JsPath \ "vidispineId").read[String] and
      (JsPath \ "status").read[String] and
      (JsPath \ "commissionId").read[String] and
      (JsPath \ "commissionTitle").read[String] and
      (JsPath \ "productionOffice").readNullable[String] and
      (JsPath \ "created").readNullable[Timestamp]
    )(MediaAtomProject.apply _)
}

case class MediaAtomProject (messageType: String, id: String, title: String, status: String, commissionId: String, commissionTitle: String, productionOffice: Option[String], created: Timestamp)
