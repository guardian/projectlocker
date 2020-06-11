package controllers

import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global
import auth.Security
import exceptions.{AlreadyExistsException, BadDataException}

/**
  * Simplified form of [[GenericDatabaseObjectControllerWithFilter]] which does not support filtering. This provides
  * stub implementations of @selectFiltered and @validateFilterParams which raise RuntimeExceptions when called.
  * @tparam M - type of the case class which represents the objects that ultimately get returned by Slick
  */
trait GenericDatabaseObjectController[M] extends GenericDatabaseObjectControllerWithFilter[M,Nothing]
{
  def selectFiltered(startAt: Int, limit:Int, terms:Nothing) = Future(Failure(new RuntimeException("Not implemented")))
  def validateFilterParams(request:Request[JsValue]) = throw new RuntimeException("Not implemented")
}

/**
  * This trait provides the JSON view code for all of the CRUD endpoints for database objects.  You need to specify two
  * type parameters, but if you don't need to support filtering it's better to extend [[GenericDatabaseObjectController]]
  * which is a subset of this trait.
  * To use it, extend this trait in your Controller and implement the indicated methods.  Then tie the routes config
  * to the indicated methods provided by this trait et voilà
  * @tparam M - type of the case class which represents the objects that ultimately get returned by Slick
  * @tparam F - type of the case class which represents supported search filter terms (the provided json is marshalled to this)
  */
trait GenericDatabaseObjectControllerWithFilter[M,F] extends InjectedController with Security {
  /**
    * Implement this method in your subclass to validate that the incoming record (passed in request) does indeed match
    * your case class.
    * Normally this can be done by simply returning: request.body.validate[YourCaseClass]. apparently this can't be done in the trait
    * because a concrete serializer implementation must be available at compile time, which would be for [YourCaseClass] but not for [M]
    * @param request Play request object
    * @return JsResult representing a validation success or failure.
    */
  def validate(request:Request[JsValue]):JsResult[M]

  /**
    * Implement this method in your subclass to validate that the incoming record (passed in request) does indeed match
    * your filter parameters case class
    * @param request Play request object
    * @return JsResult representing validation success or failure
    */
  def validateFilterParams(request:Request[JsValue]):JsResult[F]

  /**
    * Implement this method in your subclass to return a Future of all matching records
    * @param startAt start database retrieval at this record
    * @param limit limit number of returned items to this
    * @return Future of Try of Sequence of record type [[M]]
    */
  def selectall(startAt:Int, limit:Int):Future[Try[Seq[M]]]

  /**
    * Implement this method in your subclass to return a Future of all records that match the given filter terms.
    * Errors should be returned as a Failure of the provided Try
    * @param startAt start database retrieval at this record
    * @param limit limit number of returned items to this
    * @param terms case class of type [[F]] representing the filter terms
    * @return Future of Try of Sequence of record type [[M]]
    */
  def selectFiltered(startAt:Int, limit:Int, terms:F):Future[Try[Seq[M]]]
  def selectid(requestedId: Int):Future[Try[Seq[M]]]

  def deleteid(requestedId: Int):Future[Try[Int]]

  def insert(entry: M,uid:String):Future[Try[Int]]
  def dbupdate(itemId: Int, entry:M):Future[Try[Int]]

  def jstranslate(result:Seq[M]):Json.JsValueWrapper
  def jstranslate(result:M):Json.JsValueWrapper

  /**
    * Generic error handler that will return a 409 Conflict on a database conflict error or a 500 Internal Server Error
    * otherwise
    * @param error throwable representing the error
    * @param thing string describing what is being created or deleted, for error message output
    * @param isInsert boolean - true if the failed operation is an insert or create, or false if the failed operation is a delete
    * @return Play response indicating the relevant error code
    */
  def handleConflictErrors(error:Throwable, thing: String, isInsert:Boolean) = {
    val verb = if(isInsert) "create" else "delete"

    logger.error(s"Could not $verb $thing:", error)
    val errorString = error.toString
    if (errorString.contains("violates foreign key constraint") ||
        errorString.contains("Referential integrity constraint violation") ||
        errorString.contains("violates unique constraint")) {
      val errmsg = if (isInsert)
        s"This $thing either already exists or refers to objects which do not exist"
      else
        s"This $thing is still referred to by sub-objects"
      Conflict(Json.obj("status" -> "error", "detail" -> errmsg))
    } else
      InternalServerError(Json.obj("status" -> "error", "detail" -> error.toString))
  }

  /**
    * calls the callback block if the given error is a conflict error and return what it returns, or return an internalservererror
    * if the given error is not a conflict error
    * @param error
    * @param callback
    * @return
    */
  def handleConflictErrorsAdvanced(error:Throwable)(callback: =>Result):Result = {
    val errorString = error.toString
    if (errorString.contains("violates foreign key constraint") || errorString.contains("Referential integrity constraint violation")) {
      callback
    } else
      InternalServerError(Json.obj("status" -> "error", "detail" -> error.toString))
  }
  /**
    * Endpoint implementation for list, requiring auth. Link this up in route config as a GET.
    * Internally calls your [[selectall()]] implementation.
    * @param startAt query parameter representing number of record to start at
    * @param limit query parameter representing maximum number of records to return
    * @return Future of a Play Response object containing json data
    */
  def list(startAt:Int, limit: Int) = IsAuthenticatedAsync {uid=>{request=>
    selectall(startAt,limit-1).map({
      case Success(result)=>Ok(Json.obj("status"->"ok","result"->this.jstranslate(result)))
      case Failure(error)=>
        logger.error(error.toString)
        InternalServerError(Json.obj("status"->"error","detail"->error.toString))
    })
  }}

  /**
    * Endpoint implementation for listFiltered, i.e. return all records matching given filter terms
    * Filter terms are provided as a JSON request body, so link this up as a POST or PUT.
    * @param startAt query parameter representing number of record to start at
    * @param limit query parameter representing maximum number of records to return
    * @return Future of a Play Response object containing json data
    */
  def listFiltered(startAt:Int, limit:Int) = IsAuthenticatedAsync(parse.json) {uid=>{request=>
    this.validateFilterParams(request).fold(
      errors => {
        logger.error(s"errors parsing content: $errors")
        Future(BadRequest(Json.obj("status"->"error","detail"->JsError.toJson(errors))))
      },
      filterTerms => {
        this.selectFiltered(startAt, limit-1, filterTerms).map({
          case Success(result)=>Ok(Json.obj("status" -> "ok","result"->this.jstranslate(result)))
          case Failure(error)=>
            logger.error(error.toString)
            InternalServerError(Json.obj("status"->"error", "detail"->error.toString))
        }
        )
      }
    )
  }}

  /**
    * Override this method in the subclass to prevent certain entries from being created
    * @param newEntry - entry that the client wants to create
    * @return Either[String,Boolean] indicating whether to proceed or not. If Right then the operation is carried out, if Left then the string
    *         is used as the error responde detail
    */
  def shouldCreateEntry(newEntry:M):Either[String,Boolean] = Right(true)

  def innerCreate(uid:String, request:Request[JsValue]) = this.validate(request).fold(
    errors => {
      logger.error(s"errors parsing content: $errors")
      Future(BadRequest(Json.obj("status"->"error","detail"->JsError.toJson(errors))))
    },
    newEntry => {
      shouldCreateEntry(newEntry) match {
        case Right(_)=>
          this.insert(newEntry, uid).map({
            case Success(result) => Ok(Json.obj("status" -> "ok", "detail" -> "added", "id" -> result.asInstanceOf[Int]))
            case Failure(error) =>
              logger.error(error.toString)
              error match {
                case e: BadDataException =>
                  Conflict(Json.obj("status" -> "error", "detail" -> e.toString))
                case e: AlreadyExistsException =>
                  Conflict(Json.obj("status" -> "error", "detail" -> e.toString, "nextAvailableVersion"->e.getNextAvailableVersion))
                case _ =>
                  handleConflictErrors(error, "object", isInsert = true)
              }
          })
        case Left(errDetail)=>
          Future(Conflict(Json.obj("status"->"error", "detail"->errDetail)))
      }
    }
  )

  def create = IsAdminAsync(parse.json) {uid=>{request =>
    innerCreate(uid, request)
  }}

  def regularUserCreate = IsAuthenticatedAsync(parse.json) {uid=>{request=> innerCreate(uid, request)}}

  def getitem(requestedId: Int) = IsAuthenticatedAsync {uid=>{request=>
    selectid(requestedId).map({
      case Success(result)=>
        if(result.isEmpty)
         NotFound("")
        else
          Ok(Json.obj("status"->"ok","result"->this.jstranslate(result.head)))
      case Failure(error)=>
        logger.error(error.toString)
        InternalServerError(Json.obj("status"->"error","detail"->error.toString))
    })
  }}

  def update(id: Int) = IsAdminAsync(parse.json) { uid=>{request =>
    this.validate(request).fold(
      errors=>Future(BadRequest(Json.obj("status"->"error","detail"->JsError.toJson(errors)))),
      validRecord=>
        this.dbupdate(id,validRecord) map {
          case Success(rowsUpdated)=>Ok(Json.obj("status"->"ok","detail"->"Record updated", "id"->id))
          case Failure(error)=>InternalServerError(Json.obj("status"->"error", "detail"->error.toString))
        }
    )
  }}

  def deleteAction(requestedId: Int) = {
    deleteid(requestedId).map({
      case Success(result)=>
        if(result==0)
          NotFound(Json.obj("status" -> "notfound", "id"->requestedId))
        else
          Ok(Json.obj("status" -> "ok", "detail" -> "deleted", "id" -> requestedId))
      case Failure(error)=>handleConflictErrors(error,"object",isInsert=false)
    })
  }

  def delete(requestedId: Int) = IsAdminAsync {uid=>{ request =>
    if(requestedId<0)
      Future(Conflict(Json.obj("status"->"error","detail"->"This is still referenced by sub-objects")))
    else
      deleteAction(requestedId)
  }}
}
