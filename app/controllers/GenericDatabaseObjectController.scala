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
  def jstranslate(result:Seq[M]):Json.JsValueWrapper
  def jstranslate(result:M):Json.JsValueWrapper

  /**
    * Endpoint implementation for list, requiring auth. Link this up in route config as a GET.
    * Internally calls your [[selectall()]] implementation.
    * @param startAt query parameter representing number of record to start at
    * @param limit query parameter representing maximum number of records to return
    * @return Future of a Play Response object containing json data
    */
  def list(startAt:Int, limit: Int) = IsAuthenticatedAsync {uid=>{request=>
    selectall(startAt,limit).map({
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
        this.selectFiltered(startAt, limit, filterTerms).map({
          case Success(result)=>Ok(Json.obj("status" -> "ok","result"->this.jstranslate(result)))
          case Failure(error)=>
            logger.error(error.toString)
            InternalServerError(Json.obj("status"->"error", "detail"->error.toString))
        }
        )
      }
    )
  }}

  def create = IsAuthenticatedAsync(parse.json) {uid=>{request =>
    this.validate(request).fold(
      errors => {
        logger.error(s"errors parsing content: $errors")
        Future(BadRequest(Json.obj("status"->"error","detail"->JsError.toJson(errors))))
      },
      newEntry => {
        this.insert(newEntry,uid).map({
          case Success(result)=>Ok(Json.obj("status" -> "ok", "detail" -> "added", "id" -> result.asInstanceOf[Int]))
          case Failure(error)=>
            logger.error(error.toString)
            error match {
              case e:BadDataException=>
                Conflict(Json.obj("status"->"error", "detail"->e.toString))
              case e:AlreadyExistsException=>
                Conflict(Json.obj("status"->"error", "detail"->e.toString))
              case _=>
                InternalServerError(Json.obj("status"->"error", "detail"->error.toString))
            }
        }
        )
      }
    )
  }}

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

  def update(id: Int) = IsAuthenticatedAsync(parse.json) { uid=>{request =>
    this.validate(request).fold(
      errors=>Future(BadRequest(Json.obj("status"->"error","detail"->JsError.toJson(errors)))),
      StorageEntry=>Future(Ok(Json.obj("status"->"ok","detail"->"Record updated", "id"->id)))
    )
  }}

  def deleteAction(requestedId: Int) = {
    deleteid(requestedId).map({
      case Success(result)=>
        if(result==0)
          NotFound(Json.obj("status" -> "notfound", "id"->requestedId))
        else
          Ok(Json.obj("status" -> "ok", "detail" -> "deleted", "id" -> requestedId))
      case Failure(error)=>
        val errorString = error.toString
        logger.error(errorString)
        if(errorString.contains("violates foreign key constraint") || errorString.contains("Referential integrity constraint violation"))
          Conflict(Json.obj("status"->"error","detail"->"This is still referenced by sub-objects"))
        else
          InternalServerError(Json.obj("status"->"error","detail"->error.toString))
    })
  }

  def delete(requestedId: Int) = IsAuthenticatedAsync {uid=>{ request =>
    if(requestedId<0)
      Future(Conflict(Json.obj("status"->"error","detail"->"This is still referenced by sub-objects")))
    else
      deleteAction(requestedId)
  }}
}
