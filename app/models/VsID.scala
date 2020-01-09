package models

import play.api.libs.json.{JsError, JsResult, JsSuccess, Reads}

import scala.util.Try

case class VsID(siteId:String, numericId:Int)

object VsID {
  def apply(siteId: String, numericId: Int): VsID = new VsID(siteId, numericId)
  def apply(fromString:String) = {
    val idParts = fromString.split("-")
    if(idParts.length!=2) throw new RuntimeException("Invalid vidispine ID")
    val numericPart = idParts(1).toInt
    new VsID(idParts.head, numericPart)
  }
}

trait VsIDSerializer {
  private val splitterRegex = "(\\w{2})-(\\d+)".r
  implicit val vsidReads:Reads[VsID] = jsValue=>{
    val stringVal = jsValue.as[String]

    JsResult
    stringVal match {
      case splitterRegex(siteId,numericPart)=>
        JsSuccess(VsID(siteId, numericPart.toInt))
      case _=>
        JsError("This does not look like a valid Vidispine ID (not in the form XX-nnnn)")
    }
  }
}