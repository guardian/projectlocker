import java.io.{File, FileOutputStream}
import java.sql.Timestamp

import models.{ProjectEntry, ProjectEntrySerializer, ProjectTemplate, ProjectTemplateSerializer}
import org.junit.runner._
import org.specs2.runner._
import org.specs2.specification.{AfterAll, BeforeAll}
import play.api.test.Helpers._
import play.api.test._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.io.Source
import scala.util.{Failure, Success}
/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[JUnitRunner])
class FileControllerSpec extends GenericControllerSpec with BeforeAll with AfterAll with ProjectEntrySerializer with ProjectTemplateSerializer{
  sequential

  override def beforeAll() = {
    val f = new File("/tmp/realfile")
    if(!f.exists())
      new FileOutputStream(f).close()
  }

  override def afterAll() = {
    val f = new File("/tmp/realfile")
    if(f.exists())
      f.delete()
  }

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
  override val testGetDocument: String = """{"filepath":"/path/to/a/video.mxf","storage":2,"user":"me","version":1,"ctime":"2017-01-17T16:55:00.123+0000","mtime":"2017-01-17T16:55:00.123+0000","atime":"2017-01-17T16:55:00.123+0000", "hasContent": false, "hasLink": false}"""
  //the "user" parameter here is over-written by the server, so must be set to whatever the fake login is in [[GenericControllerSpec]]
  override val testCreateDocument: String =  """{"filepath":"/path/to/some/other.project","storage":1,"user":"testuser","version":3,"ctime":"2017-03-17T13:51:00.123+0000","mtime":"2017-03-17T13:51:00.123+0000","atime":"2017-03-17T13:51:00.123+0000", "hasContent": false, "hasLink": false}"""
  override val minimumNewRecordId = 6
  override val testDeleteId: Int = 3
  override val testConflictId: Int = 1

  "FileController.create" should {
    "refuse to over-write an existing record with another that has the same filename and storage" in {
      val testInvalidDocument =
        """{
          |"filepath":"/path/to/a/video.mxf",
          |"storage":2,
          |"user":"john",
          |"version":1,
          |"ctime":"2018-02-04T14:23:02.000Z",
          |"mtime":"2018-02-04T14:23:02.000Z",
          |"atime":"2018-02-04T14:23:02.000Z",
          |"hasContent": false,
          |"hasLink": false
          |}
        """.stripMargin

      val response = route(application, FakeRequest(
        method="PUT",
        uri=uriRoot,
        headers=FakeHeaders(Seq(("Content-Type", "application/json"))),
        body=testInvalidDocument).withSession("uid"->"testuser")
      ).get

      val jsondata = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]
      println(jsondata.toString)
      status(response) must equalTo(CONFLICT)

      (jsondata \ "status").as[String] mustEqual "error"
      (jsondata \ "detail").as[String] mustEqual "exceptions.AlreadyExistsException: A file already exists at /path/to/a/video.mxf on storage 2"
    }
  }

  "FileController.uploadContent" should {
    "respond 404 if data is attempted to be written to a non-existing file" in {
      val testbuffer = "this is my test data\nwith another line"
      val response = route(application, FakeRequest(
        method="PUT",
        uri="/api/file/9999/content",
        headers=FakeHeaders(Seq(("Content-Type", "application/octet-stream"))),
        body=testbuffer
      ).withSession("uid"->"testuser")).get

      status(response) mustEqual NOT_FOUND
    }

    "accept data for an existing file" in {
      val testbuffer = "this is my test data\nwith another line"
      val response = route(application, FakeRequest(
        method="PUT",
        uri="/api/file/4/content",
        headers=FakeHeaders(Seq(("Content-Type", "application/octet-stream"))),
        body=testbuffer
      ).withSession("uid"->"testuser")).get

      val responseBody = Await.result(bodyAsJsonFuture(response),10.seconds)
      println(responseBody.toString)
      status(response) mustEqual OK

      val writtenContent = Source.fromFile("/tmp/testprojectfile").getLines().mkString("\n")
      writtenContent mustEqual testbuffer
    }

    "refuse to over-write a file with existing data" in {
      val testbuffer = "this is my test data\nwith another line"
      val response = route(application, FakeRequest(
        method="PUT",
        uri="/api/file/2/content",
        headers=FakeHeaders(Seq(("Content-Type", "application/octet-stream"))),
        body=testbuffer
      ).withSession("uid"->"testuser")).get

      val responseBody = Await.result(bodyAsJsonFuture(response),10.seconds)
      status(response) mustEqual BAD_REQUEST

      (responseBody \ "status").as[String] mustEqual "error"
      (responseBody \ "detail").as[String] mustEqual "This file already has content."
    }
  }

  "FileController.references" should {
    "return lists of the things that reference a given file" in {
      val response = route(application, FakeRequest("GET","/api/file/2/associations").withSession("uid"->"testuser")).get

      status(response) mustEqual OK

      val responseBody = Await.result(bodyAsJsonFuture(response),10.seconds)

      (responseBody \ "status").as[String] mustEqual "ok"
      //this would not be a sensible configuration in the real world, but it's good for testing.
      val projectList = (responseBody \ "projects").as[List[ProjectEntry]]
      //we can't assert directly as the title value is changed by another test, which may run before or after us.
      projectList.length mustEqual 1
      projectList.head.id must beSome(2)
      projectList.head.vidispineProjectId must beSome("VX-1234")
      projectList.head.user mustEqual "you"
      projectList.head.created mustEqual Timestamp.valueOf("2016-12-11 12:21:11.021")

      (responseBody \ "templates").as[List[ProjectTemplate]].contains(ProjectTemplate(Some(3),"Some random test template",2,2, None)) must beTrue
    }
  }
}