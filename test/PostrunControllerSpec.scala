import org.junit.runner._
import org.specs2.runner._
import play.api.libs.json._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.test._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class PostrunControllerSpec extends GenericControllerSpec {
  sequential

  override val componentName: String = "PostrunController"
  override val uriRoot: String = "/api/postrun"

  override def testParsedJsonObject(checkdata: JsLookupResult, parsed_test_json: JsValue) = {
    val object_keys = Seq("runnable", "title","owner")

    object_keys.map(key=>
      (checkdata \ key).as[String] must equalTo((parsed_test_json \ key).as[String])
    )
  }

  override val testGetId: Int = 1
  override val testGetDocument: String = """{"id":1,"runnable":"FirstTestScript.py","title":"First test postrun","owner":"system","ctime":"2018-01-01T12:13:24.000"}"""
  override val testCreateDocument: String =  """{"runnable":"AnotherTestScript.py","title":"Another test script","owner":"test","version":1,"ctime":"2018-02-03T04:05:06.789Z"}"""
  override val minimumNewRecordId = 3
  override val testDeleteId: Int = 3
  override val testConflictId: Int = 1

  "PostrunActionController.associate" should {
    "create an association between postrun action and project type" in {
      val response= route(application, FakeRequest(PUT, s"$uriRoot/1/projecttype/2").withSession("uid"->"testuser")).get

      status(response) must equalTo(OK)
      val jsondata = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]
      println(jsondata.toString)
      (jsondata \ "status").as[String] must equalTo("ok")
    }
  }

  "PostrunActionController.unassociate" should {
    "remove an association between postrun action and project type" in {
      val response= route(application, FakeRequest(DELETE, s"$uriRoot/1/projecttype/1").withSession("uid"->"testuser")).get

      status(response) must equalTo(OK)
      val jsondata = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]
      println(jsondata.toString)
      (jsondata \ "status").as[String] must equalTo("ok")
    }
  }
}
