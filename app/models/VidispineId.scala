package models

import scala.util.{Failure, Success}

case class VidispineId (siteId: String, numericPart: Int) {
  override def toString: String = s"$siteId-$numericPart"
}

object VidispineId {
  val pattern = "^(\\w{2})-(\\d+)".r

  def fromString(vsId:String) = {
    try {
      val pattern(siteId, numeric) = vsId
      Success(new VidispineId(siteId, numeric.toInt))
    } catch {
      case ex:Throwable=>
        Failure(ex)
    }
  }
}