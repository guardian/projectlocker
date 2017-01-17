package controllers

import com.google.inject.Inject
import play.api.Configuration
import play.api.db.slick.DatabaseConfigProvider
import play.api.mvc.{Action, BodyParsers, Controller, Request}
import slick.driver.JdbcProfile

import scala.util.{Failure, Success}
import scala.concurrent.{Await, Future}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.sql.Timestamp

import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import models._
import org.joda.time.DateTime
import play.api.libs.json.Reads.jodaDateReads
import play.api.libs.json.Writes.jodaDateWrites

import scala.concurrent.duration._
import java.util.concurrent.TimeUnit._

import slick.lifted.TableQuery

/**
  * Created by localhome on 14/01/2017.
  */


class Files @Inject() (configuration: Configuration, dbConfigProvider: DatabaseConfigProvider)
  extends GenericDatabaseObjectController[FileEntry] with FileEntrySerializer {
  /* performs a conversion from Int to StorageEntry and back again, doing a database lookup as necessary */
  implicit val storageEntryFormat = new Format[StorageEntry] {
    def writes(s: StorageEntry): JsValue = Json.toJson(s.id)
    def reads(json: JsValue): JsResult[StorageEntry] = Json.fromJson[Int](json).map(
      storageEntryId=> Await.result({
        dbConfig.db.run(
            TableQuery[StorageEntryRow].filter(_.id === storageEntryId).result.asTry
        ).map({
          case Success(result) => result(0)
          case Failure(error) => throw error
        }
        )
      }, 2 seconds)
    )
  }

  val dbConfig = dbConfigProvider.get[JdbcProfile]

  override def selectall = dbConfig.db.run(
    TableQuery[FileEntryRow].result.asTry //simple select *
  )

  override def jstranslate(result: Seq[FileEntry]) = result  //implicit translation should handle this

  override def insert(entry: FileEntry) = dbConfig.db.run(
    (TableQuery[FileEntryRow] returning TableQuery[FileEntryRow].map(_.id) += entry).asTry
  )

  override def validate(request:Request[JsValue]) = request.body.validate[FileEntry]
}
