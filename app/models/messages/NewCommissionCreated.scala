package models.messages

import models._
import play.api.libs.json.{JsPath, Reads, Writes}
import play.api.libs.functional.syntax._

case class NewCommissionCreated(commission:PlutoCommission, timestamp: Long) extends QueuedMessage

trait NewCommissionCreatedSerializer extends PlutoCommissionSerializer with TimestampSerialization {
  implicit val newCommissionCreatedWrites:Writes[NewCommissionCreated] = (
      (JsPath \ "commission").write[PlutoCommission] and
      (JsPath \ "timestamp").write[Long]
  )(unlift(NewCommissionCreated.unapply))

  implicit val newCommissionCreatedReads:Reads[NewCommissionCreated] = (
      (JsPath \ "commission").read[PlutoCommission] and
      (JsPath \ "timestamp").read[Long]
  )(NewCommissionCreated.apply _)
}
