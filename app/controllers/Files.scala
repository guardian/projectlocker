package controllers

import java.io._
import javax.inject.Inject

import drivers.StorageDriver
import play.api.{Configuration, Logger}
import play.api.db.slick.DatabaseConfigProvider
import play.api.mvc._
import slick.driver.JdbcProfile
import play.api.libs.json._
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import models._
import slick.lifted.TableQuery

import scala.concurrent.{Future,CanAwait}
import scala.concurrent.duration._
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
          Logger.info("Got buffer")
          buffer.asBytes() match {
            case Some(bytes) => //the buffer is held in memory
              logger.debug("uploadContent: writing memory buffer")
              storageDriver.writeDataToPath(filepath, bytes.toArray)
              Ok(Json.obj("status" -> "ok", "detail" -> "File has been written."))
            case None => //the buffer is on-disk
              logger.debug("uploadContent: writing disk buffer")
              val fileInputStream = new FileInputStream(buffer.asFile)
              storageDriver.writeDataToPath(filepath, fileInputStream)
              fileInputStream.close()
              Ok(Json.obj("status" -> "ok", "detail" -> "File has been written"))
          }
        case None =>
          BadRequest(Json.obj("status" -> "error", "detail" -> "No upload payload"))
      }

  def updateFileHasContent(fileRef: FileEntry) = {
    val updateFileref = fileRef.copy(hasContent = true)

    dbConfig.db.run(
      TableQuery[FileEntryRow].filter(_.id===fileRef.id.get).update(updateFileref).asTry
    )
  }

  def uploadContent(requestedId: Int) = Action.async(parse.anyContent) { request=>
    dbConfig.db.run(
      TableQuery[FileEntryRow].filter(_.id === requestedId).result.asTry
    ).flatMap({
      case Success(rows:Seq[FileEntry])=>
        if(rows.isEmpty){
          Logger.error(s"File with ID $requestedId not found")
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
                    Logger.info(s"Writing to ${fileRef.filepath} with $storageDriver")
                    val response = writeContentIfPossible(fileRef.filepath, request.body.asRaw, storageDriver)
                    updateFileHasContent(fileRef)
                    response
                  } catch {
                    case ex: Exception =>
                      Logger.error("Unable to write file: ", ex)
                      InternalServerError(Json.obj("status" -> "error", "detail" -> s"Unable to write file: ${ex.toString}"))
                  }
                case None =>
                  Logger.error(s"No storage driver available for storage ${fileRef.storageId}")
                  InternalServerError(Json.obj("status" -> "error", "detail" -> s"No storage driver available for storage ${fileRef.storageId}"))
              }
            case Failure(error) =>
              Logger.error(s"No storage could be found for ID ${fileRef.storageId}")
              InternalServerError(Json.obj("status" -> "error", "detail" -> s"No storage could be found for ID ${fileRef.storageId}"))
          })
        }
      case Failure(error)=>
        Logger.error(s"Could not get file to write: ${error.toString}")
        Future(InternalServerError(Json.obj("status"->"error", "detail"->s"Could not get file to write: ${error.toString}")))

    })
  }
}
