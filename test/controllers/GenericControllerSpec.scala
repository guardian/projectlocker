package controllers

import org.specs2.mutable.Specification
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.junit.runner.RunWith
import org.specs2.matcher.MatchResult
import org.specs2.runner._
import play.api.test.Helpers._
import play.api.test._
import play.api.Logger
import utils.BuildMyApp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
import play.api.libs.json._

@RunWith(classOf[JUnitRunner])
trait GenericControllerSpec extends Specification with BuildMyApp {
  val logger: Logger = Logger(this.getClass)

  val componentName:String
  val uriRoot:String

  def testParsedJsonObject(checkdata:JsLookupResult,test_parsed_json:JsValue):Seq[MatchResult[Any]]

  val testGetId:Int
  val testGetDocument:String
  val testCreateDocument:String
  val testDeleteId:Int
  val testConflictId:Int
  val minimumNewRecordId:Int

  val expectedDeleteStatus = "ok"
  val expectedDeleteDetail = "deleted"

  componentName should {

    "return 400 on a bad request" in new WithApplication(buildApp) {
      logger.debug(s"$uriRoot/boum")
      val response = route(app,FakeRequest(GET, s"$uriRoot/boum")).get
      status(response) must equalTo(BAD_REQUEST)
    }

    "return valid data for a valid record" in new WithApplication(buildApp) {
      implicit val system:ActorSystem = app.actorSystem
      implicit val materializer:ActorMaterializer = ActorMaterializer()
      val response:Future[play.api.mvc.Result] = route(app, FakeRequest(GET, s"$uriRoot/1").withSession("uid"->"testuser")).get

      status(response) must equalTo(OK)
      val jsondata = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]
      (jsondata \ "status").as[String] must equalTo("ok")
      (jsondata \ "result" \ "id").as[Int] must equalTo(1)
      testParsedJsonObject(jsondata \ "result", Json.parse(testGetDocument))

    }

    "accept new data to create a new record" in new WithApplication(buildApp) {
      implicit val system:ActorSystem = app.actorSystem
      implicit val materializer:ActorMaterializer = ActorMaterializer()
      val response = route(app, FakeRequest(
        method="PUT",
        uri=uriRoot,
        headers=FakeHeaders(Seq(("Content-Type", "application/json"))),
        body=testCreateDocument).withSession("uid"->"testuser")
      ).get


      val jsondata = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]
      println(jsondata.toString)
      status(response) must equalTo(OK)
      (jsondata \ "status").as[String] must equalTo("ok")
      (jsondata \ "detail").as[String] must equalTo("added")

      (jsondata \ "id").as[Int] must greaterThanOrEqualTo(minimumNewRecordId) //if we re-run the tests without blanking the database explicitly this goes up

      val newRecordId = (jsondata \ "id").as[Int]
      val checkResponse = route(app, FakeRequest(GET, s"$uriRoot/$newRecordId").withSession("uid"->"testuser")).get
      val checkdata = Await.result(bodyAsJsonFuture(checkResponse), 5.seconds)

      (checkdata \ "status").as[String] must equalTo("ok")
      (checkdata \ "result" \ "id").as[Int] must equalTo(newRecordId)
      testParsedJsonObject(checkdata \ "result", Json.parse(testCreateDocument))
    }

    "delete a record" in new WithApplication(buildApp) {
      implicit val system:ActorSystem = app.actorSystem
      implicit val materializer:ActorMaterializer = ActorMaterializer()
      val response = route(app, FakeRequest(
        method="DELETE",
        uri=s"$uriRoot/$testDeleteId",
        headers=FakeHeaders(),
        body="").withSession("uid"->"testuser")
      ).get


      val jsondata = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]
      println(jsondata.toString)
      (jsondata \ "status").as[String] must equalTo(expectedDeleteStatus)
      (jsondata \ "detail").as[String] must equalTo(expectedDeleteDetail)
      (jsondata \ "id").as[Int] must equalTo(testDeleteId)
      status(response) must equalTo(OK)
    }

    "return conflict (409) if attempting to delete something with sub-objects" in new WithApplication(buildApp) {
      implicit val system:ActorSystem = app.actorSystem
      implicit val materializer:ActorMaterializer = ActorMaterializer()
      val response = route(app, FakeRequest(
        method = "DELETE",
        uri = s"$uriRoot/$testConflictId",
        headers = FakeHeaders(),
        body = "").withSession("uid"->"testuser")
      ).get

      status(response) must equalTo(CONFLICT)
      val jsondata = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]
      (jsondata \ "status").as[String] must equalTo("error")
    }
  }
}
