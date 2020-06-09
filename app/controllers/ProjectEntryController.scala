package controllers

import javax.inject.{Inject, Named, Singleton}
import akka.actor.ActorRef
import akka.pattern.ask
import auth.{BearerTokenAuth, Security}
import exceptions.RecordNotFoundException
import helpers.AllowCORSFunctions
import models._
import play.api.cache.SyncCacheApi
import play.api.{Configuration, Logger}
import play.api.db.slick.DatabaseConfigProvider
import play.api.http.HttpEntity
import play.api.libs.json.{JsError, JsResult, JsValue, Json}
import play.api.mvc._
import services.ValidateProject
import services.actors.creation.{CreationMessage, GenericCreationActor}
import services.actors.creation.GenericCreationActor.{NewProjectRequest, ProjectCreateTransientData}
import slick.jdbc.PostgresProfile
import slick.lifted.TableQuery
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._
import scala.language.postfixOps

@Singleton
class ProjectEntryController @Inject() (@Named("project-creation-actor") projectCreationActor:ActorRef,
                                       @Named("validate-project-actor") validateProjectActor:ActorRef,
                                        config: Configuration,
                                        dbConfigProvider: DatabaseConfigProvider,
                                        cacheImpl:SyncCacheApi,
                                        override val controllerComponents:ControllerComponents, override val bearerTokenAuth:BearerTokenAuth)
  extends GenericDatabaseObjectControllerWithFilter[ProjectEntry,ProjectEntryFilterTerms]
    with ProjectEntrySerializer with ProjectEntryAdvancedFilterTermsSerializer with ProjectRequestSerializer
    with ProjectRequestPlutoSerializer with UpdateTitleRequestSerializer with FileEntrySerializer
    with PlutoConflictReplySerializer with Security
{
  override implicit val cache:SyncCacheApi = cacheImpl

  val dbConfig = dbConfigProvider.get[PostgresProfile]
  implicit val implicitConfig = config

  override def deleteid(requestedId: Int) = dbConfig.db.run(
    TableQuery[ProjectEntryRow].filter(_.id === requestedId).delete.asTry
  )

  override def selectid(requestedId: Int):Future[Try[Seq[ProjectEntry]]] = dbConfig.db.run(
    TableQuery[ProjectEntryRow].filter(_.id === requestedId).result.asTry
  )

  protected def selectVsid(vsid: String):Future[Try[Seq[ProjectEntry]]] = dbConfig.db.run(
    TableQuery[ProjectEntryRow].filter(_.vidispineProjectId === vsid).result.asTry
  )

  override def dbupdate(itemId:Int, entry:ProjectEntry) = Future(Failure(new RuntimeException("Not implemented")))

  def getByVsid(vsid:String) = IsAuthenticatedAsync {uid=>{request=>
    selectVsid(vsid).map({
      case Success(result)=>
        if(result.isEmpty)
          NotFound(Json.obj("status"->"error","detail"->"no project with that ID"))
        else
          Ok(Json.obj("status"->"ok","result"->this.jstranslate(result)))
      case Failure(error)=>
        logger.error(error.toString)
        InternalServerError(Json.obj("status"->"error","detail"->error.toString))
    })
  }}


  /**
    * Fully generic container method to process an update request
    * @param requestedId an ID to identify what should be updated, this is passed to `selector`
    * @param selector a function that takes `requestedId` and returns a Future, containing a Try, containing a sequence of ProjectEntries
    *                 that correspond to the provided ID
    * @param f a function to perform the actual update.  This is only called if selector returns a valid sequence of at least one ProjectEntry,
    *          and is called for each ProjectEntry in the sequence that `selector` returns.
    *          It should return a Future containing a Try containing the number of rows updated.
    * @tparam T the data type of `requestedId`
    * @return A Future containing a sequnce of results for each invokation of f. with either a Failure indicating why
    *         `f` was not called, or a Success with the result of `f`
    */
  def doUpdateGenericSelector[T](requestedId:T, selector:T=>Future[Try[Seq[ProjectEntry]]])(f: ProjectEntry=>Future[Try[Int]]):Future[Seq[Try[Int]]] = selector(requestedId).flatMap({
    case Success(someSeq)=>
        if(someSeq.isEmpty)
          Future(Seq(Failure(new RecordNotFoundException(s"No records found for id $requestedId"))))
        else
          Future.sequence(someSeq.map(f))
    case Failure(error)=>Future(Seq(Failure(error)))
  })

  /**
    * Most updates are done with the primary key, this is a convenience method to call [[doUpdateGenericSelector]]
    * with the appropriate selector and data type for the primary key
    * @param requestedId integer primary key value identifying what should be updated
    * @param f a function to perform the actual update. See [[doUpdateGenericSelector]] for details
    * @return see [[doUpdateGenericSelector]]
    */
  def doUpdateGeneric(requestedId:Int)(f: ProjectEntry=>Future[Try[Int]]) = doUpdateGenericSelector[Int](requestedId,selectid)(f)

  /**
    * Update the vidisipineId on a data record
    * @param requestedId primary key of the record to update
    * @param newVsid new vidispine ID. Note that this is an Option[String] as the id can be null
    * @return a Future containing a Try containing an Int describing the number of records updated
    */
  def doUpdateVsid(requestedId:Int, newVsid:Option[String]):Future[Seq[Try[Int]]] = doUpdateGeneric(requestedId){ record=>
    val updatedProjectEntry = record.copy (vidispineProjectId = newVsid)
    dbConfig.db.run (
      TableQuery[ProjectEntryRow].filter (_.id === requestedId).update (updatedProjectEntry).asTry
    )
  }

  /**
    * generic code for an endpoint to update the title
    * @param requestedId identifier of the record to update
    * @param updater function to perform the actual update.  This is passed requestedId and a string to change the title to
    * @tparam T type of @reqestedId
    * @return a Future[Response]
    */
  def genericUpdateTitleEndpoint[T](requestedId:T)(updater:(T,String)=>Future[Seq[Try[Int]]]) = IsAuthenticatedAsync(parse.json) {uid=>{request=>
    request.body.validate[UpdateTitleRequest].fold(
      errors=>
        Future(BadRequest(Json.obj("status"->"error", "detail"->JsError.toJson(errors)))),
      updateTitleRequest=> {
        val results = updater(requestedId, updateTitleRequest.newTitle).map(_.partition(_.isSuccess))

        results.map(resultTuple => {
          val failures = resultTuple._2
          val successes = resultTuple._1

          if (failures.isEmpty)
            Ok(Json.obj("status" -> "ok", "detail" -> s"${successes.length} record(s) updated"))
          else
            genericHandleFailures(failures, requestedId)
        })
      }
    )
  }}

  /**
    * endpoint to update project title field of record based on primary key
    * @param requestedId
    * @return
    */
  def updateTitle(requestedId:Int) = genericUpdateTitleEndpoint[Int](requestedId) { (requestedId,newTitle)=>
    doUpdateGeneric(requestedId) {record=>
      val updatedProjectEntry = record.copy (projectTitle = newTitle)
      dbConfig.db.run (
        TableQuery[ProjectEntryRow].filter (_.id === requestedId).update (updatedProjectEntry).asTry
      )
    }
  }

  /**
    * endoint to update project title field of record based on vidispine id
    * @param vsid
    * @return
    */
  def updateTitleByVsid(vsid:String) = genericUpdateTitleEndpoint[String](vsid) { (vsid,newTitle)=>
    doUpdateGenericSelector[String](vsid,selectVsid) { record=> //this lambda function is called once for each record
      val updatedProjectEntry = record.copy(projectTitle = newTitle)
      dbConfig.db.run(
        TableQuery[ProjectEntryRow].filter(_.id === record.id.get).update(updatedProjectEntry).asTry
      )
    }
  }


  def genericHandleFailures[T](failures:Seq[Try[Int]], requestedId:T) = {
    val notFoundFailures = failures.filter(_.failed.get.getClass==classOf[RecordNotFoundException])

    if(notFoundFailures.length==failures.length) {
      NotFound(Json.obj("status" -> "error", "detail" -> s"no records found for $requestedId"))
    } else {
      InternalServerError(Json.obj("status" -> "error", "detail" -> failures.map(_.failed.get.toString)))
    }
  }

  def updateVsid(requestedId:Int) = IsAuthenticatedAsync(parse.json) {uid=>{request=>
    request.body.validate[UpdateTitleRequest].fold(
      errors=>
        Future(BadRequest(Json.obj("status"->"error", "detail"->JsError.toJson(errors)))),
      updateTitleRequest=>{
        val results = doUpdateVsid(requestedId, updateTitleRequest.newVsid).map(_.partition(_.isSuccess))

        results.map(resultTuple => {
          val failures = resultTuple._2
          val successes = resultTuple._1

          if (failures.isEmpty)
            Ok(Json.obj("status" -> "ok", "detail" -> s"${successes.length} record(s) updated"))
          else {
            genericHandleFailures(failures, requestedId)
          }
        })
      }
    )
  }}

  def filesList(requestedId: Int) = IsAuthenticatedAsync {uid=>{request=>
    implicit val db = dbConfig.db

    selectid(requestedId).flatMap({
      case Failure(error)=>
        logger.error(s"could not list files from project ${requestedId}",error)
        Future(InternalServerError(Json.obj("status"->"error","detail"->error.toString)))
      case Success(someSeq)=>
        someSeq.headOption match { //matching on pk, so can only be one result
          case Some(projectEntry)=>
            projectEntry.associatedFiles.map(fileList=>Ok(Json.obj("status"->"ok","files"->fileList)))
          case None=>
            Future(NotFound(Json.obj("status"->"error","detail"->s"project $requestedId not found")))
        }
    })
  }}

  override def selectall(startAt:Int, limit:Int) = dbConfig.db.run(
    TableQuery[ProjectEntryRow].sortBy(_.created.desc).drop(startAt).take(limit).result.asTry //simple select *
  )

  override def selectFiltered(startAt: Int, limit: Int, terms: ProjectEntryFilterTerms): Future[Try[Seq[ProjectEntry]]] = {
    dbConfig.db.run(
      terms.addFilterTerms {
        TableQuery[ProjectEntryRow]
      }.sortBy(_.created.desc).drop(startAt).take(limit).result.asTry
    )
  }

  override def jstranslate(result: Seq[ProjectEntry]):Json.JsValueWrapper = result
  override def jstranslate(result: ProjectEntry):Json.JsValueWrapper = result  //implicit translation should handle this

  /*this is pointless because of the override of [[create]] below, so it should not get called,
   but is needed to conform to the [[GenericDatabaseObjectController]] protocol*/
  override def insert(entry: ProjectEntry,uid:String) = Future(Failure(new RuntimeException("ProjectEntryController::insert should not have been called")))

  override def validate(request:Request[JsValue]) = request.body.validate[ProjectEntry]

  override def validateFilterParams(request: Request[JsValue]): JsResult[ProjectEntryFilterTerms] = request.body.validate[ProjectEntryFilterTerms]

  def createFromFullRequest(rq:ProjectRequestFull) = {
    implicit val timeout:akka.util.Timeout = 60.seconds

    val initialData = ProjectCreateTransientData(None, None, None)

    val msg = NewProjectRequest(rq,None,initialData)
    (projectCreationActor ? msg).mapTo[CreationMessage].map({
      case GenericCreationActor.ProjectCreateSucceeded(succeededRequest, projectEntry)=>
        logger.info(s"Created new project: $projectEntry")
        Ok(Json.obj("status"->"ok","detail"->"created project", "projectId"->projectEntry.id.get))
      case GenericCreationActor.ProjectCreateFailed(failedRequest, error)=>
        logger.error("Could not create new project", error)
        InternalServerError(Json.obj("status"->"error","detail"->error.toString))
    })
  }

  override def create = IsAuthenticatedAsync(parse.json) {uid=>{ request =>
    implicit val db = dbConfig.db

    request.body.validate[ProjectRequest].fold(
      errors=>
        Future(BadRequest(Json.obj("status"->"error","detail"->JsError.toJson(errors)))),
      projectRequest=> {
        val fullRequestFuture=projectRequest.copy(user=uid).hydrate
        fullRequestFuture.flatMap({
          case None=>
            Future(BadRequest(Json.obj("status"->"error","detail"->"Invalid template or storage ID")))
          case Some(rq)=>
            createFromFullRequest(rq)
        })
      })
  }}

  def handleMatchingProjects(rq:ProjectRequestFull, matchingProjects:Seq[ProjectEntry], force: Boolean):Future[Result] = {
    implicit val db = dbConfig.db
    logger.info(s"Got matching projects: $matchingProjects")
    if (matchingProjects.nonEmpty) {
      if (!force) {
        Future.sequence(matchingProjects.map(proj => PlutoConflictReply.getForProject(proj)))
          .map(plutoMatch => Conflict(Json.obj("status" -> "conflict","detail"->"projects already exist", "result" -> plutoMatch)))
      } else {
        logger.info("Conflicting projects potentially exist, but continuing anyway as force=true")
        createFromFullRequest(rq)
      }
    } else {
      logger.info("No matching projects, creating")
      createFromFullRequest(rq)
    }
  }

  def createExternal(force:Boolean) = IsAuthenticatedAsync(parse.json) {uid=>{ request:Request[JsValue] =>
    implicit val db = dbConfig.db

    request.body.validate[ProjectRequestPluto].fold(
      errors=>
        Future(BadRequest(Json.obj("status"->"error","detail"->JsError.toJson(errors)))),
      projectRequest=>
        projectRequest.hydrate.flatMap({
          case Left(errorList)=>
            Future(BadRequest(Json.obj("status"->"error","detail"->errorList)))
          case Right(rq)=>
            if(rq.existingVidispineId.isDefined){
              ProjectEntry.lookupByVidispineId(rq.existingVidispineId.get).flatMap({
                case Success(matchingProjects)=>
                  handleMatchingProjects(rq, matchingProjects, force)
                case Failure(error)=>
                  logger.error("Unable to look up vidispine ID: ", error)
                  Future(InternalServerError(Json.obj("status"->"error","detail"->error.toString)))
              })
            } else {
              createFromFullRequest(rq)
            }
        }).recover({
          case err:Throwable=>
            logger.error(s"Could not run createExternal for ${request.body}", err)
            InternalServerError(Json.obj("status"->"error", "detail"->err.toString))
        })
    )
  }}

  def getDistinctOwnersList:Future[Try[Seq[String]]] = {
    //work around distinctOn bug - https://github.com/slick/slick/issues/1712
    dbConfig.db.run(sql"""select distinct(s_user) from "ProjectEntry"""".as[String].asTry)
  }

  def distinctOwners = IsAuthenticatedAsync {uid=>{request=>
    getDistinctOwnersList.map({
      case Success(ownerList)=>
        Ok(Json.obj("status"->"ok","result"->ownerList))
      case Failure(error)=>
        logger.error("Could not look up distinct project owners: ", error)
        InternalServerError(Json.obj("status"->"error","detail"->error.toString))
    })
  }}

  def performFullValidation = IsAuthenticatedAsync {uid=>{request=>
    implicit val actorTimeout:akka.util.Timeout = 55 seconds  //loadbalancer ususally times out after 60

    (validateProjectActor ? ValidateProject.ValidateAllProjects).mapTo[ValidateProject.VPMsg].map({
      case ValidateProject.ValidationSuccess(totalProjects, projectCount, failedProjects)=>
        Ok(Json.obj("status"->"ok","totalProjectsCount"->totalProjects,"failedProjectsList"->failedProjects))
      case ValidateProject.ValidationError(err)=>
        InternalServerError(Json.obj("status"->"error","detail"->err.toString))
    }).recover({
      case err:Throwable=>
        logger.error(s"Error calling ValidateProject actor: ", err)
        InternalServerError(Json.obj("status"->"error", "detail"->err.toString))
    })
  }}

  def advancedSearch(startAt:Int,limit:Int) = IsAuthenticatedAsync(parse.json) { uid=> request=>
    request.body.validate[ProjectEntryAdvancedFilterTerms] fold(
      errors=>
        Future(BadRequest(Json.obj("status"->"error", "detail"->JsError.toJson(errors)))),
      filterTerms=>{
        val queryFut = dbConfig.db.run(
          filterTerms.addFilterTerms {
            TableQuery[ProjectEntryRow]
          }.sortBy(_._1.created.desc).drop(startAt).take(limit).map(_._1).result)

          queryFut
            .map(results=>Ok(Json.obj("status" -> "ok","result"->this.jstranslate(results))))
          .recover({
            case err:Throwable=>
              logger.error(s"Could not perform advanced search for $filterTerms: ", err)
              InternalServerError(Json.obj("status"->"error","detail"->err.toString))
          })
      }
    )
  }

  /**
    * respond to CORS options requests for login from vaultdoor
    * see https://developer.mozilla.org/en-US/docs/Glossary/Preflight_request
    * @return
    */
  def searchOptions = Action { request=>
    AllowCORSFunctions.checkCorsOrigins(config, request) match {
      case Right(allowedOrigin) =>
        val returnHeaders = Map(
          "Access-Control-Allow-Methods" -> "PUT, OPTIONS",
          "Access-Control-Allow-Origin" -> allowedOrigin,
          "Access-Control-Allow-Headers" -> "content-type",
        )
        Result(
          ResponseHeader(204, returnHeaders),
          HttpEntity.NoEntity
        )
      case Left(other) =>
        logger.warn(s"Invalid CORS preflight request for authentication: $other")
        Forbidden("")
    }
  }
}
