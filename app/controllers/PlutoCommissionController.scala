package controllers

import akka.actor.ActorSystem
import akka.stream.Materializer
import exceptions.{AlreadyExistsException, BadDataException}
import javax.inject._
import models._
import play.api.cache.SyncCacheApi
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.{JsError, JsResult, JsValue, Json}
import play.api.mvc.{EssentialAction, Request, ResponseHeader, Result}
import slick.jdbc.{GetResult, JdbcProfile, PostgresProfile}
import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global
import akka.stream.alpakka.slick.scaladsl._
import akka.stream.scaladsl._
import akka.util.ByteString
import play.api.http.HttpEntity

@Singleton
class PlutoCommissionController @Inject()(dbConfigProvider:DatabaseConfigProvider, cacheImpl:SyncCacheApi)(implicit system:ActorSystem,mat:Materializer)
  extends GenericDatabaseObjectControllerWithFilter[PlutoCommission,PlutoCommissionFilterTerms]
    with PlutoCommissionSerializer with PlutoCommissionFilterTermsSerializer {

    private val databaseConfig = dbConfigProvider.get[PostgresProfile]
    implicit val db = databaseConfig.db
    implicit val cache:SyncCacheApi = cacheImpl

    //this is used by Alpakka for the streaming interface
    implicit val session = SlickSession.forConfig("slick.dbs.default")    //connect to the default database
    system.registerOnTermination(()=>session.close())

    override def selectall(startAt: Int, limit: Int): Future[Try[Seq[PlutoCommission]]] = db.run(
      TableQuery[PlutoCommissionRow].drop(startAt).take(limit).sortBy(_.title.asc).result.asTry
    )

    override def selectid(requestedId: Int): Future[Try[Seq[PlutoCommission]]] = db.run(
      TableQuery[PlutoCommissionRow].filter(_.id===requestedId).result.asTry
    )

    override def selectFiltered(startAt: Int, limit: Int, terms: PlutoCommissionFilterTerms): Future[Try[Seq[PlutoCommission]]] = db.run(
        terms.addFilterTerms {
            TableQuery[PlutoCommissionRow]
        }.drop(startAt).take(limit).sortBy(_.title.asc.nullsLast).result.asTry
    )

    def doCreateCommissionRecord(newEntry:PlutoCommission, uid:String) =
        shouldCreateEntry(newEntry) match {
            case Right(_) =>
                this.insert(newEntry, uid).map({
                    case Success(result) =>
                        logger.info(s"Successfully created commission record ${result.asInstanceOf[Int]} for ${newEntry.title}")
                        Ok(Json.obj("status" -> "ok", "detail" -> "added", "id" -> result.asInstanceOf[Int]))
                    case Failure(error) =>
                        logger.error(error.toString)
                        error match {
                            case e: BadDataException =>
                                Conflict(Json.obj("status" -> "error", "detail" -> e.toString))
                            case e: AlreadyExistsException =>
                                Conflict(Json.obj("status" -> "error", "detail" -> e.toString))
                            case _ =>
                                handleConflictErrors(error, "object", isInsert = true)
                        }
                })
            case Left(errDetail) =>
                Future(Conflict(Json.obj("status" -> "error", "detail" -> errDetail)))
        }

    /**
      * override the standard create method. This is necessary because the [[PlutoCommission]] object contains the
      * Projectlocker integer key of the working group, but Pluto does not know about it. So we must convert it here,
      * using PlutoCommission.fromServerRepresentation.  This is why we can't use the standard JsValue.validate[object]
      * form.
      * @return
      */
    override def create = IsAuthenticatedAsync(parse.json) { uid => { request =>
        try {
            val siteId = (request.body \ "siteId").as[String]
            val workingGroupUuid = (request.body \ "gnm_commission_workinggroup").as[String]

            PlutoWorkingGroup.entryForUuid(workingGroupUuid).flatMap({
                case Some(workingGroup) =>
                    PlutoCommission.fromServerRepresentation(request.body, workingGroup.id.get, siteId) match {
                        case Success(newEntry) =>
                            logger.debug(s"Got pluto commission object $newEntry")
                            doCreateCommissionRecord(newEntry, uid)
                        case Failure(error) =>
                            logger.error(s"Could not look up working group for $workingGroupUuid", error)
                            Future(InternalServerError(Json.obj("status" -> "error", "detail" -> error.toString)))
                    }
                case None =>
                    Future(BadRequest(Json.obj("status" -> "error", "detail" -> "Working group does not exist")))
            })
        } catch {
            case e:Throwable=>
                logger.error("Could not process create commission request:", e)
                Future(InternalServerError(Json.obj("status"->"error","detail"->e.toString)))
        }
    }
    }

    override def insert(entry: PlutoCommission, uid: String): Future[Try[Int]] = db.run(
        (TableQuery[PlutoCommissionRow] returning TableQuery[PlutoCommissionRow].map(_.id) += entry).asTry)

    override def deleteid(requestedId: Int):Future[Try[Int]] = throw new RuntimeException("This is not supported")

    override def dbupdate(itemId: Int, entry:PlutoCommission):Future[Try[Int]] = throw new RuntimeException("This is not supported")

    /*these are handled through implict translation*/
    override def jstranslate(result:Seq[PlutoCommission]):Json.JsValueWrapper = result
    override def jstranslate(result:PlutoCommission):Json.JsValueWrapper = result

    override def validate(request: Request[JsValue]): JsResult[PlutoCommission] = request.body.validate[PlutoCommission]

    override def validateFilterParams(request: Request[JsValue]): JsResult[PlutoCommissionFilterTerms] = request.body.validate[PlutoCommissionFilterTerms]

    def streamingSelectAll = IsAuthenticated {uid=>{request=>
      val src = Slick.source(TableQuery[PlutoCommissionRow].sortBy(_.title.asc).result)
        .map(entry=>Json.toJson(entry))
        .map(json=>ByteString(Json.toBytes(json)).concat(ByteString("\n")))

      Result(
          header = ResponseHeader(200,Map.empty),
          body = HttpEntity.Streamed(src,None,Some("application/x-ndjson"))
      )
    }}

    def listFilteredStream = IsAuthenticatedAsync(parse.json) {uid=>{request=>
        this.validateFilterParams(request).fold(
            errors => {
                logger.error(s"errors parsing content: $errors")
                Future(BadRequest(Json.obj("status"->"error","detail"->JsError.toJson(errors))))
            },
            filterTerms => {
                val src = Slick.source(
                    filterTerms.addFilterTerms{ TableQuery[PlutoCommissionRow] }.sortBy(_.title.asc.nullsLast).result
                ).map(entry=>Json.toJson(entry))
                  .map(json=>ByteString(Json.toBytes(json)).concat(ByteString("\n")))

                Future(Result(
                    header = ResponseHeader(200,Map.empty),
                    body = HttpEntity.Streamed(src,None,Some("application/x-ndjson"))
                ))
            }
        )
    }}
  }
