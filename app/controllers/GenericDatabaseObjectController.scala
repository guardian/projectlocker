package controllers
import java.sql.Timestamp

import models.{FileEntry, FileEntryRow}
import org.joda.time.DateTime
import play.api.libs.json.Reads.jodaDateReads
import play.api.libs.json.Writes.jodaDateWrites
import play.api.libs.json._
import play.api.mvc.{Action, BodyParsers, Controller}

import scala.concurrent.Future
import scala.util.{Failure, Success}


trait GenericDatabaseObjectController extends Controller {
  def timestampToDateTime(t: Timestamp): DateTime = new DateTime(t.getTime)
  def dateTimeToTimestamp(dt: DateTime): Timestamp = new Timestamp(dt.getMillis)
  implicit val dateWrites = jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss.SSSZ") //this DOES take numeric timezones - Z means Zone, not literal letter Z
  implicit val dateReads = jodaDateReads("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

  /* performs a conversion from java.sql.Timestamp to Joda DateTime and back again */
  implicit val timestampFormat = new Format[Timestamp] {
    def writes(t: Timestamp): JsValue = Json.toJson(timestampToDateTime(t))
    def reads(json: JsValue): JsResult[Timestamp] = Json.fromJson[DateTime](json).map(dateTimeToTimestamp)
  }

  def list:play.api.mvc.Action[play.api.mvc.AnyContent]

  def create:play.api.mvc.Action[play.api.libs.json.JsValue]

  def update(id: Int):play.api.mvc.Action[play.api.libs.json.JsValue]

  def delete(id: Int):play.api.mvc.Action[play.api.libs.json.JsValue]

}
