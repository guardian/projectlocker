import javax.inject.Inject

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.test._
import play.api.inject.bind
import testHelpers.TestDatabase
import play.api.{Application, Logger}
import play.api.http.HttpEntity.Strict

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
import play.api.libs.json._

@RunWith(classOf[JUnitRunner])
class StorageControllerSpec extends Specification {
  //needed for body.consumeData
  implicit val system = ActorSystem("storage-controller-spec")
  implicit val materializer = ActorMaterializer()

  val logger: Logger = Logger(this.getClass)

  //can over-ride bindings here. see https://www.playframework.com/documentation/2.5.x/ScalaTestingWithGuice
  val application:Application = new GuiceApplicationBuilder()
    .overrides(bind[DatabaseConfigProvider].to[TestDatabase.testDbProvider])
    .build

  def bodyAsJsonFuture(response:Future[play.api.mvc.Result]) = response.flatMap(result=>
    result.body.consumeData.map(contentBytes=> {
      logger.debug(contentBytes.decodeString("UTF-8"))
      Json.parse(contentBytes.decodeString("UTF-8"))
    }
    )
  )

  "StorageController" should {

    "return 400 on a bad request" in {
      val response = route(application,FakeRequest(GET, "/storage/boum")).get

      status(response) must equalTo(BAD_REQUEST)
    }

    "return valid data for a valid storage" in  {
      val response:Future[play.api.mvc.Result] = route(application, FakeRequest(GET, "/storage/1")).get

      val jsondata = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]
      (jsondata \ "status").as[String] must equalTo("ok")
      (jsondata \ "result" \ "id").as[Int] must equalTo(1)
      (jsondata \ "result" \ "storageType").as[String] must equalTo("filesystem")
      (jsondata \ "result" \ "user").as[String] must equalTo("me")
      status(response) must equalTo(OK)
    }

    "accept new data to create a new storage" in {
      val test_json = """{"storageType": "ftp", "user": "tests"}"""
      val response = route(application, FakeRequest(
        method="PUT",
        uri="/storage",
        headers=FakeHeaders(Seq(("Content-Type", "application/json"))),
        body=test_json)
      ).get

      status(response) must equalTo(OK)
      val jsondata = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]
      (jsondata \ "status").as[String] must equalTo("ok")
      (jsondata \ "detail").as[String] must equalTo("added")
      (jsondata \ "id").as[Int] must greaterThan(3) //if we re-run the tests without blanking the database explicitly this goes up

      val newRecordId = (jsondata \ "id").as[Int]
      val checkResponse = route(application, FakeRequest(GET, s"/storage/$newRecordId")).get
      val checkdata = Await.result(bodyAsJsonFuture(checkResponse), 5.seconds)

      val parsed_test_json = Json.parse(test_json)

      (checkdata \ "status").as[String] must equalTo("ok")
      (checkdata \ "result" \ "id").as[Int] must equalTo(newRecordId)
      (checkdata \ "result" \ "storageType").as[String] must equalTo((parsed_test_json \ "storageType").as[String])
      (checkdata \ "result" \ "user").as[String] must equalTo((parsed_test_json \ "user").as[String])
    }

    "delete a storage" in {
      val response = route(application, FakeRequest(
        method="DELETE",
        uri="/storage/4",
        headers=FakeHeaders(),
        body="")
      ).get

      status(response) must equalTo(OK)
      val jsondata = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]
      (jsondata \ "status").as[String] must equalTo("ok")
      (jsondata \ "detail").as[String] must equalTo("deleted")
      (jsondata \ "id").as[Int] must equalTo(4)
    }

    "return conflict (409) if attempting to delete something with sub-objects" in {
      val response = route(application, FakeRequest(
        method="DELETE",
        uri="/storage/2",
        headers=FakeHeaders(),
        body="")
      ).get

      status(response) must equalTo(CONFLICT)
      val jsondata = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]
      (jsondata \ "status").as[String] must equalTo("error")
      (jsondata \ "detail").as[String] must equalTo("This is still referenced by sub-objects")
    }
  }
}
