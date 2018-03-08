import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.junit.runner._
import org.specs2.matcher.MatchResult
import org.specs2.mutable._
import org.specs2.runner._
import play.api.cache.SyncCacheApi
import play.api.cache.ehcache.EhCacheModule
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.{Injector, bind}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.test._
import play.api.{Application, Logger}
import testHelpers.TestDatabase

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
trait GenericControllerSpec extends Specification with MockedCacheApi{
  //needed for body.consumeData
  implicit val system = ActorSystem("storage-controller-spec")
  implicit val materializer = ActorMaterializer()

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

  //can over-ride bindings here. see https://www.playframework.com/documentation/2.5.x/ScalaTestingWithGuice
  val application:Application = new GuiceApplicationBuilder().disable(classOf[EhCacheModule])
    .overrides(bind[DatabaseConfigProvider].to[TestDatabase.testDbProvider])
    .overrides(bind[SyncCacheApi].toInstance(mockedSyncCacheApi))
    .build

  def bodyAsJsonFuture(response:Future[play.api.mvc.Result]) = response.flatMap(result=>
    result.body.consumeData.map(contentBytes=> {
      logger.debug(contentBytes.decodeString("UTF-8"))
      Json.parse(contentBytes.decodeString("UTF-8"))
    }
    )
  )

  componentName should {

    "return 400 on a bad request" in {
      logger.debug(s"$uriRoot/boum")
      val response = route(application,FakeRequest(GET, s"$uriRoot/boum")).get
      status(response) must equalTo(BAD_REQUEST)
    }

    "return valid data for a valid record" in  {

      val response:Future[play.api.mvc.Result] = route(application, FakeRequest(GET, s"$uriRoot/1").withSession("uid"->"testuser")).get

      status(response) must equalTo(OK)
      val jsondata = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]
      (jsondata \ "status").as[String] must equalTo("ok")
      (jsondata \ "result" \ "id").as[Int] must equalTo(1)
      testParsedJsonObject(jsondata \ "result", Json.parse(testGetDocument))

    }

    "accept new data to create a new record" in {
      val response = route(application, FakeRequest(
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
      val checkResponse = route(application, FakeRequest(GET, s"$uriRoot/$newRecordId").withSession("uid"->"testuser")).get
      val checkdata = Await.result(bodyAsJsonFuture(checkResponse), 5.seconds)

      (checkdata \ "status").as[String] must equalTo("ok")
      (checkdata \ "result" \ "id").as[Int] must equalTo(newRecordId)
      testParsedJsonObject(checkdata \ "result", Json.parse(testCreateDocument))
    }

    "delete a record" in {
      val response = route(application, FakeRequest(
        method="DELETE",
        uri=s"$uriRoot/$testDeleteId",
        headers=FakeHeaders(),
        body="").withSession("uid"->"testuser")
      ).get

      status(response) must equalTo(OK)
      val jsondata = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]
      (jsondata \ "status").as[String] must equalTo(expectedDeleteStatus)
      (jsondata \ "detail").as[String] must equalTo(expectedDeleteDetail)
      (jsondata \ "id").as[Int] must equalTo(testDeleteId)
    }

    "return conflict (409) if attempting to delete something with sub-objects" in {
      val response = route(application, FakeRequest(
        method = "DELETE",
        uri = s"$uriRoot/$testConflictId",
        headers = FakeHeaders(),
        body = "").withSession("uid"->"testuser")
      ).get

      status(response) must equalTo(CONFLICT)
      val jsondata = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]
      (jsondata \ "status").as[String] must equalTo("error")
      (jsondata \ "detail").as[String] must equalTo("This object is still referred to by sub-objects")
    }
  }
}
