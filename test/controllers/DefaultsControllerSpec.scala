package controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import models.{Defaults, DefaultsSerializer}
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.Logger
import play.api.cache.SyncCacheApi
import play.api.cache.ehcache.EhCacheModule
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import play.api.test.Helpers._
import play.api.test._
import slick.jdbc.JdbcProfile
import testHelpers.TestDatabase
import utils.{BuildMyApp, MockedCacheApi}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

@RunWith(classOf[JUnitRunner])
class DefaultsControllerSpec extends Specification with BuildMyApp with DefaultsSerializer {
  tag("controllers")
  sequential

  private val logger=Logger(getClass)

  "DefaultsController.getForKey" should {
    "return data for a valid key" in new WithApplication(buildApp) {
      //needed for body.consumeData
      implicit val system:ActorSystem = app.actorSystem
      implicit val materializer = ActorMaterializer()

      //needed for database access
      private val injector = app.injector
      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val response = route(app, FakeRequest(GET, "/api/default/lunch").withSession("uid"->"testuser")).get
      status(response) must equalTo(OK)

      val result = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]
      (result \ "status").as[String] mustEqual "ok"
      (result \ "result" \ "value").as[String] mustEqual "sandwich"
    }

    "return a 404 if key does not exist" in new WithApplication(buildApp) {
      //needed for body.consumeData
      implicit val system:ActorSystem = app.actorSystem
      implicit val materializer = ActorMaterializer()

      //needed for database access
      private val injector = app.injector
      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val response = route(app, FakeRequest(GET, "/api/default/snack").withSession("uid"->"testuser")).get
      status(response) must equalTo(NOT_FOUND)

      val result = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]
      (result \ "status").as[String] mustEqual "notfound"
    }
  }

  "DefaultsController.putForKey" should {
    "write a new value to the database" in new WithApplication(buildApp) {
      //needed for body.consumeData
      implicit val system:ActorSystem = app.actorSystem
      implicit val materializer = ActorMaterializer()

      //needed for database access
      private val injector = app.injector
      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val initialValue = Await.result(Defaults.entryFor("dinner"),5.seconds)
      initialValue must beSuccessfulTry[Option[Defaults]](None)

      val response = route(app, FakeRequest(PUT,
        "/api/default/dinner",
        body = "meatballs",
        headers=FakeHeaders(Seq(("Content-Type", "text/plain")))
      ).withSession("uid"->"testuser")).get
      status(response) must equalTo(OK)

      val result = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]
      (result \ "status").as[String] mustEqual "ok"
      (result \ "result" \ "value").as[String] mustEqual "meatballs"

      val finalValue = Await.result(Defaults.entryFor("dinner"),5.seconds)
      finalValue must beSuccessfulTry
      finalValue.get must beSome
      finalValue.get.get.name mustEqual "dinner"
      finalValue.get.get.id must beSome
      finalValue.get.get.value mustEqual "meatballs"
    }

    "update an existing value to the database" in new WithApplication(buildApp) {
      //needed for body.consumeData
      implicit val system:ActorSystem = app.actorSystem
      implicit val materializer = ActorMaterializer()

      //needed for database access
      private val injector = app.injector
      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val initialValue = Await.result(Defaults.entryFor("breakfast"),5.seconds)
      initialValue must beSuccessfulTry[Option[Defaults]](Some(Defaults(Some(2),"breakfast","toast")))

      val response = route(app, FakeRequest(PUT,
        "/api/default/breakfast",
        body = "scrambled eggs",
        headers=FakeHeaders(Seq(("Content-Type", "text/plain")))
      ).withSession("uid"->"testuser")).get
      status(response) must equalTo(OK)

      val result = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]
      (result \ "status").as[String] mustEqual "ok"
      (result \ "result" \ "value").as[String] mustEqual "scrambled eggs"

      val finalValue = Await.result(Defaults.entryFor("breakfast"),5.seconds)
      finalValue must beSuccessfulTry[Option[Defaults]](Some(Defaults(Some(2),"breakfast","scrambled eggs")))
    }
  }

  "DefaultsController.deleteForKey" should {
    "delete an existing value from the database" in new WithApplication(buildApp) {
      //needed for body.consumeData
      implicit val system:ActorSystem = app.actorSystem
      implicit val materializer = ActorMaterializer()

      //needed for database access
      private val injector = app.injector
      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val initialValue = Await.result(Defaults.entryFor("dessert"),5.seconds)
      initialValue must beSuccessfulTry[Option[Defaults]](Some(Defaults(Some(3),"dessert","nothing")))

      val response = route(app, FakeRequest(DELETE, "/api/default/dessert").withSession("uid"->"testuser")).get

      status(response) must equalTo(OK)

      val finalValue = Await.result(Defaults.entryFor("dessert"),5.seconds)
      finalValue must beSuccessfulTry[Option[Defaults]](None)
    }

    "return 404 if the requested key does not exist" in new WithApplication(buildApp) {
      //needed for body.consumeData
      implicit val system:ActorSystem = app.actorSystem
      implicit val materializer = ActorMaterializer()

      //needed for database access
      private val injector = app.injector
      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val response = route(app, FakeRequest(DELETE, "/api/default/underpants").withSession("uid"->"testuser")).get

      status(response) must equalTo(NOT_FOUND)
    }
  }

  "DefaultsController.list" should {
    "return a list of all defaults values" in new WithApplication(buildApp) {
      //needed for body.consumeData
      implicit val system:ActorSystem = app.actorSystem
      implicit val materializer = ActorMaterializer()

      //needed for database access
      private val injector = app.injector
      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val response = route(app, FakeRequest(GET, "/api/default").withSession("uid"->"testuser")).get
      status(response) must equalTo(OK)

      val result = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]
      (result \ "status").as[String] mustEqual "ok"
      val resultList = (result \ "results").as[Seq[Defaults]]
      resultList mustEqual Seq(
        Defaults(Some(1),"lunch","sandwich"),
        Defaults(Some(4),"project_storage_id","1"),
        Defaults(Some(5),"dinner","meatballs"),
        Defaults(Some(2),"breakfast","scrambled eggs")
      )
    }
  }
}
