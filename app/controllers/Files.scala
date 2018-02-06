package controllers

import java.io._

import com.google.inject.Inject
import drivers.StorageDriver
import play.api.{Configuration, Logger}
import play.api.db.slick.DatabaseConfigProvider
import play.api.mvc._
import slick.driver.JdbcProfile
import play.api.libs.json._
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import models._
import play.mvc.BodyParser.AnyContent
import play.mvc.Http.RequestBody
import slick.lifted.TableQuery

import scala.concurrent.Future
import scala.io.Source
import scala.util.{Failure, Success}


class Files @Inject() (configuration: Configuration, dbConfigProvider: DatabaseConfigProvider)
  extends GenericDatabaseObjectController[FileEntry] with FileEntrySerializer {

  val dbConfig = dbConfigProvider.get[JdbcProfile]

  override def deleteid(requestedId: Int) = dbConfig.db.run(
    TableQuery[FileEntryRow].filter(_.id === requestedId).delete.asTry
  )

  override def selectid(requestedId: Int) = {
    println("In select")
    dbConfig.db.run(
      TableQuery[FileEntryRow].filter(_.id === requestedId).result.asTry
    )
  }

  override def selectall = dbConfig.db.run(
    TableQuery[FileEntryRow].result.asTry //simple select *
  )

  override def jstranslate(result: Seq[FileEntry]) = result  //implicit translation should handle this
  override def jstranslate(result: FileEntry) = result  //implicit translation should handle this

  override def insert(entry: FileEntry) = dbConfig.db.run(
    (TableQuery[FileEntryRow] returning TableQuery[FileEntryRow].map(_.id) += entry).asTry
  )

  override def validate(request:Request[JsValue]) = request.body.validate[FileEntry]

  def writeContentIfPossible(filepath:String, maybeBuffer: Option[RawBuffer], storageDriver:StorageDriver):Result = maybeBuffer match {
        case Some(buffer) =>
          buffer.asBytes() match {
            case Some(bytes) => //the buffer is held in memory
              logger.debug("uploadContent: writing memory buffer")
              storageDriver.writeDataToPath(filepath, bytes.toArray)
              Ok(Json.obj("status" -> "ok", "detail" -> "File has been written"))
            case None => //the buffer is on-disk
              logger.debug("uploadContent: writing disk buffer")
              val fileInputStream = new BufferedInputStream(new FileInputStream(buffer.asFile))
              storageDriver.writeDataToPath(filepath, fileInputStream)
              fileInputStream.close()
              Ok(Json.obj("status" -> "ok", "detail" -> "File has been written"))
          }
        case None =>
          BadRequest(Json.obj("status" -> "error", "detail" -> "No upload payload"))
      }

  def uploadContent(requestedId: Int) = Action.async(parse.anyContent) { request=>
    println("In uploadContent")
    dbConfig.db.run(
      TableQuery[FileEntryRow].filter(_.id === requestedId).result.asTry
    ).flatMap({
      case Success(rows:Seq[FileEntry])=>
        if(rows.isEmpty){
          Future(NotFound(Json.obj("status"->"error", "detail"->s"File with ID $requestedId not found")))
        } else {
          val fileRef = rows.head
          //get the storage reference for the file
          val storageResult = dbConfig.db.run(TableQuery[StorageEntryRow].filter(_.id === fileRef.storageId).result.asTry)

          storageResult.map({
            case Success(storages: Seq[StorageEntry]) =>
              storages.head.getStorageDriver match {
                case Some(storageDriver) =>
                  try {
                    writeContentIfPossible(fileRef.filepath, request.body.asRaw, storageDriver)
                    Ok(Json.obj("status"->"ok","detail"->"File has been written"))
                  } catch {
                    case ex: Exception =>
                      Logger.error("Unable to write file: ", ex)
                      InternalServerError(Json.obj("status" -> "error", "detail" -> s"Unable to write file: ${ex.toString}"))
                  }
                case None =>
                  InternalServerError(Json.obj("status" -> "error", "detail" -> s"No storage driver available for storage ${fileRef.storageId}"))
              }
            case Failure(error) =>
              InternalServerError(Json.obj("status" -> "error", "detail" -> s"Unexpected object return"))
          })
        }
      case Failure(error)=>
        Future(InternalServerError(Json.obj("status"->"error", "detail"->s"Could not get file to write: ${error.toString}")))

    })
  }
}
