package controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.junit.runner._
import org.specs2.runner._
import play.api.libs.json._
import play.api.test.Helpers._
import play.api.test._

import scala.concurrent.Await
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class ProjectTypeControllerSpec extends GenericControllerSpec {
  sequential

  override val componentName: String = "ProjectTypeController"
  override val uriRoot: String = "/api/projecttype"

  override def testParsedJsonObject(checkdata: JsLookupResult, parsed_test_json: JsValue) = {
    val object_keys = Seq("name","opensWith","targetVersion")

    object_keys.map(key=>
      (checkdata \ key).as[String] must equalTo((parsed_test_json \ key).as[String])
    )
  }

  override val testGetId: Int = 1
  override val testGetDocument: String = """{"name":"Premiere 2014 test","opensWith":"AdobePremierePro.app","targetVersion":"14.0","fileExtension":".prproj"}"""
  override val testCreateDocument: String =  """{"name":"My Wonderful Editor","opensWith":"MyWonderfulEditor.app","targetVersion":"3.6","fileExtension":".edd"}"""
  override val minimumNewRecordId = 4
  override val testDeleteId: Int = 3
  override val testConflictId: Int = -1

  "ProjectTypeController.listPostrun" should {
    "return a list of the postrun ids associated with the project type" in new WithApplication(buildApp){
      implicit val system:ActorSystem = app.actorSystem
      implicit val materializer:ActorMaterializer = ActorMaterializer()
      val response = route(app, FakeRequest(
        method = "GET",
        uri = s"$uriRoot/$testGetId/postrun",
        headers = FakeHeaders(),
        body = "").withSession("uid"->"testuser")
      ).get

      status(response) must equalTo(OK)
      val jsondata = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]

      (jsondata \ "status").as[String] mustEqual "ok"
      (jsondata \ "result").as[Seq[Int]] mustEqual Seq(1, 2, 5)
    }
  }
}
