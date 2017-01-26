import org.junit.runner._
import org.specs2.runner._
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.test._
import play.api.inject.bind
import testHelpers.TestDatabase
import play.api.{Application, Logger}
import play.api.http.HttpEntity.Strict
import org.specs2.mutable._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.actor._
import akka.stream.ActorMaterializer

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

  "StorageController" should {

    "return 400 on a bad request" in {
      val response = route(application,FakeRequest(GET, "/storage/boum")).get

      status(response) must equalTo(BAD_REQUEST)
    }

    "return valid data for a valid storage" in  {
      val response = route(application, FakeRequest(GET, "/storage/1")).get

      val jsonFuture = response.flatMap(result=>
        result.body.consumeData.map(contentBytes=> {
          logger.debug(contentBytes.decodeString("UTF-8"))
          Json.parse(contentBytes.decodeString("UTF-8"))
        }
        )
      )

      val jsondata = Await.result(jsonFuture, 5.seconds).as[JsValue]
      (jsondata \ "status").as[String] must equalTo("ok")
      (jsondata \ "result" \ "id").as[Int] must equalTo(1)
      (jsondata \ "result" \ "storageType").as[String] must equalTo("filesystem")

      status(response) must equalTo(OK)
    }
  }
  override val testGetId: Int = 1
  override val testGetDocument: String = """{"storageType": "filesystem", "user": "me"}"""
  override val testCreateDocument: String =  """{"storageType": "ftp", "user": "tests"}"""
  override val testDeleteId: Int = 2
  override val testConflictId: Int = 1
  override val minimumNewRecordId: Int = 2
}
