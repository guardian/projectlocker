package models

import java.sql.Timestamp

import org.joda.time.DateTime
import play.api.libs.json._

trait TimestampSerialization {
  def timestampToDateTime(t: Timestamp): DateTime = new DateTime(t.getTime)
  def dateTimeToTimestamp(dt: DateTime): Timestamp = new Timestamp(dt.getMillis)
  implicit val dateWrites = JodaWrites.jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss.SSSZ") //this DOES take numeric timezones - Z means Zone, not literal letter Z
  implicit val dateReads = JodaReads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

  /**
    *  performs a conversion from java.sql.Timestamp to Joda DateTime and back again
    */
  implicit val timestampFormat = new Format[Timestamp] {
    def writes(t: Timestamp): JsValue = Json.toJson(timestampToDateTime(t))
    def reads(json: JsValue): JsResult[Timestamp] = Json.fromJson[DateTime](json).map(dateTimeToTimestamp)
  }
}
