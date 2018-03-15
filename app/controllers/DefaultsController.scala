package controllers

import javax.inject._

import auth.Security
import models.{Defaults, DefaultsSerializer}
import play.api.{Configuration, Logger}
import play.api.cache.SyncCacheApi
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json._
import play.api.mvc.{AbstractController, ControllerComponents}
import slick.jdbc.PostgresProfile

import scala.util.{Failure, Success}
import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class DefaultsController @Inject() (cc:ControllerComponents, configuration: Configuration,
                                    dbConfigProvider: DatabaseConfigProvider, cacheImpl:SyncCacheApi)
  extends AbstractController(cc) with Security with DefaultsSerializer {
  override val logger = Logger(getClass)

  implicit val cache = cacheImpl
  implicit val db = dbConfigProvider.get[PostgresProfile].db

  /**
    * Action to return the value for the given defaults key
    * @param key key to look up
    * @return 200 with the data as a json object if found, 404 if not found, 500 if an error
    */
  def getForKey(key:String) = IsAuthenticatedAsync {uid=>{request=>
    Defaults.entryFor(key).map({
      case Success(maybeResult)=>
        maybeResult match {
          case Some(result)=>Ok(Json.obj("status"->"ok","result"->result))
          case None=>NotFound(Json.obj("status"->"notfound"))
        }
      case Failure(error)=>
        logger.error(s"Could not look up defaults key $key: ", error)
        InternalServerError(Json.obj("status"->"error","detail"->error.toString))
    })
  }}

  /**
    * Action to set the given defaults key
    * @param key key to set
    * @return 200 with the created/updated record as a json object or 500 if an error
    */
  def putForKey(key:String) = IsAuthenticatedAsync(parse.text) {uid=>{request=>
    Defaults.entryFor(key).flatMap({
      case Success(maybeResult)=>
        maybeResult match {
          case Some(result)=>
            result.copy(value=request.body).save map {
              case Success(newRecord)=>Ok(Json.obj("status"->"ok","result"->newRecord))
              case Failure(error)=>
                logger.error("Unable to update existing defaults record: ", error)
                InternalServerError(Json.obj("status"->"error","detail"->error.toString))
            }
          case None=>
            Defaults(None,key,request.body).save map {
              case Success(newRecord)=>Ok(Json.obj("status"->"ok","result"->newRecord))
              case Failure(error)=>
                logger.error("Unable to save new defaults record: ", error)
                InternalServerError(Json.obj("status"->"error","detail"->error.toString))
            }
        }
      case Failure(error)=>
        logger.error("Unable to look up defaults record: ", error)
        Future(InternalServerError(Json.obj("status"->"error","detail"->error.toString)))
    })
  }}

  /**
    * Action to delete the given defaults key
    * @param key key to delete
    * @return 200 if the operation succeeded, 404 if the key was not found or 500 if there was an error
    */
  def deleteForKey(key:String) = IsAuthenticatedAsync {uid=>{request=>
    Defaults.entryFor(key).flatMap({
      case Success(maybeResult)=>
        maybeResult match {
          case Some(result)=>
            result.delete map {
              case Success(newRecord)=>Ok(Json.obj("status"->"ok","result"->newRecord))
              case Failure(error)=>
                logger.error("Unable to update existing defaults record: ", error)
                InternalServerError(Json.obj("status"->"error","detail"->error.toString))
            }
          case None=>Future(NotFound(Json.obj("status"->"notfound")))
        }
      case Failure(error)=>
        logger.error("Unable to look up defaults record: ", error)
        Future(InternalServerError(Json.obj("status"->"error","detail"->error.toString)))
    })
  }}

  def list = IsAuthenticatedAsync {uid=>{request=>
    Defaults.allEntries.map({
      case Success(results)=>Ok(Json.obj("status"->"ok","results"->results))
      case Failure(error)=>
        logger.error("could not list all defaults values: ", error)
        InternalServerError(Json.obj("status"->"error","detail"->error.toString))
    })
  }}
}
