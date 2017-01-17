package controllers
import java.sql.Timestamp
import java.util.concurrent.Callable

import models.{GenericModel, GenericModelRow}
import org.joda.time.DateTime
import play.api.libs.json.Reads.jodaDateReads
import play.api.libs.json.Writes.jodaDateWrites
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{Action, BodyParsers, Controller}
import slick.lifted.TableQuery
import slick.driver.PostgresDriver.api._

import scala.concurrent.Future
import scala.util.{Failure, Success}
import play.api.Configuration
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.functional.syntax.unlift
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global

trait GenericDatabaseObjectController[M <: GenericModel,R <: GenericModelRow[M]] extends Controller {
  /*these are injected in the constructor of an implementing class*/
  val configuration:Configuration
  val dbConfigProvider:DatabaseConfigProvider
  val tableTag:Tag=>R

  val objects = TableQuery(tableTag)

  val dbConfig = dbConfigProvider.get[JdbcProfile]

  def timestampToDateTime(t: Timestamp): DateTime = new DateTime(t.getTime)
  def dateTimeToTimestamp(dt: DateTime): Timestamp = new Timestamp(dt.getMillis)

  implicit val dateWrites = jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss.SSSZ") //this DOES take numeric timezones - Z means Zone, not literal letter Z
  implicit val dateReads = jodaDateReads("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

  /* performs a conversion from java.sql.Timestamp to Joda DateTime and back again */
  implicit val timestampFormat = new Format[Timestamp] {
    def writes(t: Timestamp): JsValue = Json.toJson(timestampToDateTime(t))
    def reads(json: JsValue): JsResult[Timestamp] = Json.fromJson[DateTime](json).map(dateTimeToTimestamp)
  }

  def companionOf[T : Manifest] : Option[AnyRef] = try{
    val classOfT = implicitly[Manifest[T]].erasure
    val companionClassName = classOfT.getName + "$"
    val companionClass = Class.forName(companionClassName)
    val moduleField = companionClass.getField("MODULE$")
    Some(moduleField.get(null))
  } catch {
    case e => None
  }

  /*https://www.playframework.com/documentation/2.5.x/ScalaJson*/
  implicit val mappingWrites:Writes[M] = (
    (JsPath \ "id").writeNullable[Int] and
      (JsPath \ "name").write[String]
  )(unlift(GenericModel.unapply))

  implicit val mappingReads:Reads[M] = (
    (JsPath \ "id").readNullable[Int] and
      (JsPath \ "name").read[String]
  )(companionOf[M].get.asInstanceOf[scala.Function].apply _)

  def create = Action.async(BodyParsers.parse.json) { request =>
    request.body.validate[M].fold(
      errors => {
        Future(BadRequest(Json.obj("status"->"error","detail"->JsError.toJson(errors))))
      },
      ModelEntry => {
        dbConfig.db.run(
          (objects returning objects.map(_.id) += ModelEntry).asTry
        ).map({
          case Success(result)=>Ok(Json.obj("status" -> "ok", "detail" -> "added", "id" -> result))
          case Failure(error)=>InternalServerError(Json.obj("status"->"error", "detail"->error.toString))
        }
        )
      }
    )
  }

  def list = Action.async {
    dbConfig.db.run(
      objects.result.asTry //simple select *
    ).map({
      case Success(result)=>Ok(Json.obj("status"->"ok","result"->result))
      case Failure(error)=>InternalServerError(Json.obj("status"->"error","detail"->error.toString))
    })
  }

  def update(id: Int) = Action.async(BodyParsers.parse.json) { request =>
    request.body.validate[R].fold(
      errors=>Future(BadRequest(Json.obj("status"->"error","detail"->JsError.toJson(errors)))),
      FileEntry=>Future(Ok(""))
    )
  }

  def delete(id: Int) = Action.async(BodyParsers.parse.json) { request =>
    Future(Ok(""))
  }

}
