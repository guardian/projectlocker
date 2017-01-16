package controllers

import com.google.inject.Inject
import models.{StorageEntry, StorageEntryRow}
import play.api.Configuration
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{Action, BodyParsers}
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.PostgresDriver.api._

import scala.concurrent.Future
import scala.util.{Failure, Success}

import scala.concurrent.ExecutionContext.Implicits.global

class StoragesController @Inject()
    (override val configuration: Configuration, override val dbConfigProvider: DatabaseConfigProvider)
    extends GenericDatabaseObjectController[StorageEntry,StorageEntryRow] {

  /*https://www.playframework.com/documentation/2.5.x/ScalaJson*/
  implicit val mappingWrites:Writes[StorageEntry] = (
    (JsPath \ "id").writeNullable[Int] and
      (JsPath \ "rootpath").writeNullable[String] and
      (JsPath \ "storageType").write[String] and
      (JsPath \ "user").writeNullable[String] and
      (JsPath \ "password").writeNullable[String] and
      (JsPath \ "host").writeNullable[String] and
      (JsPath \ "port").writeNullable[Int]
  )(unlift(StorageEntry.unapply))

  implicit val mappingReads:Reads[StorageEntry] = (
    (JsPath \ "id").readNullable[Int] and
      (JsPath \ "rootpath").readNullable[String] and
      (JsPath \ "storageType").read[String] and
      (JsPath \ "user").readNullable[String] and
      (JsPath \ "password").readNullable[String] and
      (JsPath \ "host").readNullable[String] and
      (JsPath \ "port").readNullable[Int]
    )(StorageEntry.apply _)

}
