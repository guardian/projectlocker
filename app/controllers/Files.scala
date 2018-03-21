package controllers

import javax.inject.Inject

import exceptions.{AlreadyExistsException, BadDataException}
import helpers.StorageHelper
import play.api.{Configuration, Logger}
import play.api.db.slick.DatabaseConfigProvider
import play.api.mvc._
import slick.jdbc.PostgresProfile
import play.api.libs.json._
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import models._
import play.api.cache.SyncCacheApi
import play.mvc.Http.Response
import slick.lifted.TableQuery

import scala.concurrent.{CanAwait, Future}
import scala.util.{Failure, Success, Try}


class Files @Inject() (configuration: Configuration, dbConfigProvider: DatabaseConfigProvider, cacheImpl:SyncCacheApi)
  extends GenericDatabaseObjectControllerWithFilter[FileEntry,FileEntryFilterTerms]
    with FileEntrySerializer with FileEntryFilterTermsSerializer
    with ProjectEntrySerializer with ProjectTemplateSerializer {

  implicit val cache:SyncCacheApi = cacheImpl
  val storageHelper = new StorageHelper

  val dbConfig = dbConfigProvider.get[PostgresProfile]
  implicit val db = dbConfig.db

  override def deleteid(requestedId: Int) = dbConfig.db.run(
    TableQuery[FileEntryRow].filter(_.id === requestedId).delete.asTry
  )

  override def selectid(requestedId: Int) = {
    dbConfig.db.run(
      TableQuery[FileEntryRow].filter(_.id === requestedId).result.asTry
    )
  }

  override def selectall(startAt:Int, limit:Int) = dbConfig.db.run(
    TableQuery[FileEntryRow].drop(startAt).take(limit).result.asTry //simple select *
  )

  override def validateFilterParams(request: Request[JsValue]): JsResult[FileEntryFilterTerms] = request.body.validate[FileEntryFilterTerms]

  override def selectFiltered(startAt: Int, limit: Int, terms: FileEntryFilterTerms): Future[Try[Seq[FileEntry]]] = {
    dbConfig.db.run(
      terms.addFilterTerms {
        TableQuery[FileEntryRow]
      }.drop(startAt).take(limit).result.asTry
    )
  }

  override def jstranslate(result: Seq[FileEntry]) = result //implicit translation should handle this
  override def jstranslate(result: FileEntry) = result //implicit translation should handle this

  override def insert(entry: FileEntry,uid:String):Future[Try[Int]] = {
    /* only allow a record to be created if no files already exist with that path on that storage */
    FileEntry.entryFor(entry.filepath,entry.storageId)(dbConfig.db).flatMap({
      case Success(fileList)=>
        if(fileList.isEmpty){
          val updatedEntry = entry.copy(user = uid)
          dbConfig.db.run(
            (TableQuery[FileEntryRow] returning TableQuery[FileEntryRow].map(_.id) += updatedEntry).asTry
          )
        } else {
          Future(Failure(new AlreadyExistsException(s"A file already exists at ${entry.filepath} on storage ${entry.storageId}")))
        }
      case Failure(error)=>Future(Failure(error))
    })

  }

  override def dbupdate(itemId:Int, entry:FileEntry) = {
    val newRecord = entry.id match {
      case Some(id)=>entry
      case None=>entry.copy(id=Some(itemId))
    }

    dbConfig.db.run(
      TableQuery[FileEntryRow].filter(_.id===itemId).update(newRecord).asTry
    )
  }

  override def validate(request: Request[JsValue]) = request.body.validate[FileEntry]

  def uploadContent(requestedId: Int) = IsAuthenticatedAsync(parse.anyContent) {uid=>{ request =>
    implicit val db = dbConfig.db

    request.body.asRaw match {
      case Some(buffer) =>
        dbConfig.db.run(
          TableQuery[FileEntryRow].filter(_.id === requestedId).result.asTry
        ).flatMap({
          case Success(rows: Seq[FileEntry]) =>
            if (rows.isEmpty) {
              logger.error(s"File with ID $requestedId not found")
              Future(NotFound(Json.obj("status" -> "error", "detail" -> s"File with ID $requestedId not found")))
            } else {
              val fileRef = rows.head
              //get the storage reference for the file
              if(fileRef.hasContent)
                Future(BadRequest(Json.obj("status"->"error","detail"->"This file already has content.")))
              else
                fileRef.writeToFile(buffer).map({
                  case Success(x) =>
                    Ok(Json.obj("status" -> "ok", "detail" -> "File has been written."))
                  case Failure(error) =>
                    InternalServerError(Json.obj("status" -> "error", "detail" -> error.toString))
                })
            }
          case Failure(error) =>
            logger.error(s"Could not get file to write: ${error.toString}")
            Future(InternalServerError(Json.obj("status" -> "error", "detail" -> s"Could not get file to write: ${error.toString}")))
        })
      case None =>
        Future(BadRequest(Json.obj("status" -> "error", "detail" -> "No upload payload")))
    }
  }}

  def deleteFromDisk(requestedId:Int, targetFile:FileEntry, deleteReferenced: Boolean, isRetry:Boolean=false):Future[Result] = deleteid(requestedId).flatMap({
    case Success(rowCount)=>
      storageHelper.deleteFile(targetFile).flatMap({
        case true =>
          targetFile.getFullPath.map(fullpath=> {
            Ok(Json.obj("status" -> "ok", "detail" -> "deleted", "filepath" -> fullpath, "id" -> requestedId))
          })
        case false =>
          targetFile.getFullPath.map(fullpath=>{
            logger.error(s"Could not delete on-disk file $fullpath")
            InternalServerError(Json.obj("status" -> "error", "detail" -> "could not delete file on disk", "filepath" -> fullpath, "id"->requestedId))
          })
      })
    case Failure(error)=>Future(handleConflictErrorsAdvanced(error){
        Conflict(Json.obj("status"->"error","detail"->"This file is still referenced by other things"))
    })
  })

  def delete(requestedId: Int, deleteReferenced: Boolean) = IsAdminAsync {uid=>{ request =>
    selectid(requestedId).flatMap({
      case Success(rowSeq)=>
        rowSeq.headOption match {
          case Some(targetFile)=>
            deleteFromDisk (requestedId, targetFile, deleteReferenced)
          case None=>
            logger.error("No file found")
            Future(NotFound(Json.obj("status"->"error", "detail"->s"nothing found in database for $requestedId")))
        }
      case Failure(error)=>
        logger.error("Could not look up file id: ", error)
        Future(InternalServerError(Json.obj("status"->"error", "detail"->"could not look up file id", "error"->error.toString)))
    })
  }}

  def references(requestedId: Int) = IsAdminAsync {uid=>{request=>
    Future.sequence(Seq(FileAssociation.projectsForFile(requestedId),ProjectTemplate.templatesForFileId(requestedId))).map(resultSeq=>{
      val triedProjectsList = resultSeq.head.asInstanceOf[Try[Seq[ProjectEntry]]]
      val triedTemplatesList = resultSeq(1).asInstanceOf[Try[Seq[ProjectTemplate]]]

      if(triedProjectsList.isSuccess && triedTemplatesList.isSuccess)
        Ok(Json.obj("status"->"ok","projects"->triedProjectsList.get, "templates"->triedTemplatesList.get))
      else
        InternalServerError(Json.obj("status"->"error",
          "projectsError"->triedProjectsList.failed.getOrElse("").toString,
          "templatesError"->triedTemplatesList.failed.getOrElse("").toString
        ))
    }
    )
  }}
}

