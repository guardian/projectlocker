import org.junit.runner._
import org.specs2.runner._
import play.api.Application
import play.api.mvc.{AnyContent, Call}
import play.api.test._
import play.test.Helpers._
import play.api.mvc.Call._
import play.mvc.Http.RequestBuilder

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
import play.api.libs.json._

@RunWith(classOf[JUnitRunner])
class FileControllerSpec extends GenericControllerSpec  {
  sequential

  override val componentName: String = "FileController"
  override val uriRoot: String = "/api/file"

  override def testParsedJsonObject(checkdata: JsLookupResult, parsed_test_json: JsValue) = {
    val object_keys = Seq("filepath","user","ctime","mtime","atime")
    val object_keys_int = Seq("storage","version")

    object_keys.map(key=>
      (checkdata \ key).as[String] must equalTo((parsed_test_json \ key).as[String])
    ) ++ object_keys_int.map(key=>
      (checkdata \ key).as[Int] must equalTo((parsed_test_json \ key).as[Int])
    )
  }

  override val testGetId: Int = 1
  override val testGetDocument: String = """{"filepath":"/path/to/a/video.mxf","storage":2,"user":"me","version":1,"ctime":"2017-01-17T16:55:00.123+0000","mtime":"2017-01-17T16:55:00.123+0000","atime":"2017-01-17T16:55:00.123+0000"}"""
  override val testCreateDocument: String =  """{"filepath":"/path/to/some/other.project","storage":1,"user":"test","version":3,"ctime":"2017-03-17T13:51:00.123+0000","mtime":"2017-03-17T13:51:00.123+0000","atime":"2017-03-17T13:51:00.123+0000"}"""
  override val minimumNewRecordId = 4
  override val testDeleteId: Int = 3
  override val testConflictId: Int = 1
}


class FileControllerPlaySpec extends PlaySpecification {
  "FileController" should {

    "respond 404 if data is attempted to be written to a non-existing file" in new WithServer {
      val testbuffer = "this is my test data\nwith another line"
      //val call = Call(GET, "/")
      //val req = FakeRequest(PUT, "/api/file/9/content", FakeHeaders(), testbuffer.toCharArray.map(_.toByte))
      //val call2 = Call(PUT,"/api/file/9/content", testbuffer)

      //      val req = fakeRequest()
      //              .method("PUT")
      //              .uri("/api/file/9/content")
      //              .bodyRaw(testbuffer.toCharArray.map(_.toByte))
      //                .build()
      //
      //      val result:Future[play.api.mvc.Result] = route(app, req._underlyingRequest()).get
      //
      //      //result must beSome[Future[Result]]
      //
      //      result.onComplete({
      //        case Success(finalResult:play.api.mvc.Result)=>
      //          finalResult.header.status mustEqual 200
      //        case Failure(error)=>
      //          throw error
      //      })
      //      Await.ready(result, 60.seconds)
      WsTestClient.withClient(client => {
        val request = client.url("http://localhost:19001/api/file/9/content")
        val response = Await.result(request.put(testbuffer.toCharArray.map(_.toByte)),30.seconds)
        response.status mustEqual 404
      })
    }

    "accept data for an existing file" in new WithServer() {
      val testbuffer = "this is my test data\nwith another line"

      WsTestClient.withClient(client => {
        val request = client.url("http://localhost:19001/api/file/3/content")
        val response = Await.result(request.put(testbuffer.toCharArray.map(_.toByte)),30.seconds)

        println(response.body)
        response.status mustEqual 200
      })

      val writtenContent = Source.fromFile("/tmp/testproject2").getLines().mkString("\n")
      writtenContent mustEqual testbuffer
    }
  }
}