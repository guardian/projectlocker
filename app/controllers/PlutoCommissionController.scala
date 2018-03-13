package controllers

import javax.inject._

import models._
import play.api.cache.SyncCacheApi
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.{JsResult, JsValue, Json}
import play.api.mvc.Request
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future
import scala.util.Try

@Singleton
class PlutoCommissionController @Inject()(dbConfigProvider:DatabaseConfigProvider, cacheImpl:SyncCacheApi)
  extends GenericDatabaseObjectControllerWithFilter[PlutoCommission,PlutoCommissionFilterTerms]
    with PlutoCommissionSerializer with PlutoCommissionFilterTermsSerializer {

    implicit val db = dbConfigProvider.get[JdbcProfile].db
    implicit val cache:SyncCacheApi = cacheImpl

    override def selectall(startAt: Int, limit: Int): Future[Try[Seq[PlutoCommission]]] = db.run(
      TableQuery[PlutoCommissionRow].drop(startAt).take(limit).sortBy(_.updated.desc).result.asTry
    )

    override def selectid(requestedId: Int): Future[Try[Seq[PlutoCommission]]] = db.run(
      TableQuery[PlutoCommissionRow].filter(_.id===requestedId).result.asTry
    )

    override def selectFiltered(startAt: Int, limit: Int, terms: PlutoCommissionFilterTerms): Future[Try[Seq[PlutoCommission]]] = db.run(
        terms.addFilterTerms {
            TableQuery[PlutoCommissionRow]
        }.drop(startAt).take(limit).result.asTry
    )
    override def insert(entry: PlutoCommission, uid: String): Future[Try[Int]] = throw new RuntimeException("This is not supported")

    override def deleteid(requestedId: Int):Future[Try[Int]] = throw new RuntimeException("This is not supported")

    override def dbupdate(itemId: Int, entry:PlutoCommission):Future[Try[Int]] = throw new RuntimeException("This is not supported")

    /*these are handled through implict translation*/
    override def jstranslate(result:Seq[PlutoCommission]):Json.JsValueWrapper = result
    override def jstranslate(result:PlutoCommission):Json.JsValueWrapper = result

    override def validate(request: Request[JsValue]): JsResult[PlutoCommission] = request.body.validate[PlutoCommission]

    override def validateFilterParams(request: Request[JsValue]): JsResult[PlutoCommissionFilterTerms] = request.body.validate[PlutoCommissionFilterTerms]


  }
