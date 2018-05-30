package controllers

import java.sql.Timestamp
import java.time.LocalDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import helpers.{ProjectCreateHelper, ProjectCreateHelperImpl}
import models._
import org.specs2.matcher.ThrownExpectations
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import play.api.test._
import slick.jdbc.JdbcProfile
import testHelpers.TestDatabase

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Success

class ProjectEntryControllerSpec extends Specification with utils.BuildMyApp with ThrownExpectations with Mockito with ProjectEntrySerializer with PlutoConflictReplySerializer {
  sequential

//  "ProjectEntryController.create" should {
//    "validate request data and call out to ProjectCreationActor" in new WithApplication(buildAppWithMockedProjectHelper){
//      implicit val system:ActorSystem = app.actorSystem
//      implicit val materializer:ActorMaterializer = ActorMaterializer()
//      //needed for database access
//      private val injector = app.injector
//      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
//      private implicit val db = dbConfigProvider.get[JdbcProfile].db
//
//      val testCreateDocument =
//        """
//          |{
//          |  "filename": "sometestprojectfile",
//          |  "destinationStorageId": 1,
//          |  "title": "MyTestProjectEntry",
//          |  "projectTemplateId": 1,
//          |  "user": "test-user"
//          |}
//        """.stripMargin
//
//      val fakeProjectEntry = ProjectEntry(Some(999),1,None,"MyTestProjectEntry",Timestamp.valueOf(LocalDateTime.now()),"test-user",None,None)
//      mockedProjectHelper.create(any[ProjectRequestFull],org.mockito.Matchers.eq(None))(org.mockito.Matchers.eq(db),org.mockito.Matchers.any[play.api.Configuration]) answers((arglist,mock)=>Future(Success(fakeProjectEntry)))
//      val response = route(app, FakeRequest(
//        method="PUT",
//        uri="/api/project",
//        headers=FakeHeaders(Seq(("Content-Type", "application/json"))),
//        body=testCreateDocument).withSession("uid"->"testuser")).get
//
//      val jsondata = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]
//      println(jsondata.toString)
//      (jsondata \ "status").as[String] must equalTo("ok")
//      (jsondata \ "projectId").as[Int] must equalTo(999)
//      (jsondata \ "detail").as[String] must equalTo("created project")
//      status(response) must equalTo(OK)
//    }
//  }

  "ProjectEntryController.createFromPluto" should {
    "validate request and show errors if any data does not line up" in new WithApplication(buildApp){
      implicit val system:ActorSystem = app.actorSystem
      implicit val materializer:ActorMaterializer = ActorMaterializer()
      //needed for database access
      private val injector = app.injector
      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val testCreateDocument =
        """{"commissionVSID": "VX-1447",
          |"filename":"VX-1747",
          |"plutoProjectTypeUuid": "330b4d84-ef24-4102-b093-0d15829afa64",
          |"title": "projectlocker test 4",
          |"user": "andy_gallagher",
          |"workingGroupUuid": "1b97c363-fba0-4771-90b5-9bd65aaed306",
          |"vidispineId":"VX-1747"}
        """.stripMargin

      val fakeProjectEntry = ProjectEntry(Some(999),1,None,"MyTestProjectEntry",Timestamp.valueOf(LocalDateTime.now()),"test-user",None,None)
      mockedProjectHelper.create(any[ProjectRequestFull],org.mockito.Matchers.eq(None))(org.mockito.Matchers.eq(db),org.mockito.Matchers.any[play.api.Configuration]) answers((arglist,mock)=>Future(Success(fakeProjectEntry)))
      val response = route(app, FakeRequest(
        method="PUT",
        uri="/api/project/external/create",
        headers=FakeHeaders(Seq(("Content-Type", "application/json"))),
        body=testCreateDocument).withSession("uid"->"testuser")).get

      val jsondata = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]
      println(jsondata.toString)
      status(response) must equalTo(BAD_REQUEST)


      (jsondata \ "status").as[String] must equalTo("error")
      (jsondata \ "detail").as[Seq[String]] must equalTo(Seq("No record could be found for the pluto project type 330b4d84-ef24-4102-b093-0d15829afa64","No working group could be found for 1b97c363-fba0-4771-90b5-9bd65aaed306","No commission could be found for VX-1447"))
    }

    "return a Conflict containing all matching projects and their likely pluto types if a project with the same vsid exists already" in new WithApplication(buildApp){
      implicit val system:ActorSystem = app.actorSystem
      implicit val materializer:ActorMaterializer = ActorMaterializer()
      //needed for database access
      private val injector = app.injector
      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val testCreateDocument =
        """{"commissionVSID": "VX-4567",
          |"filename":"VX-2345",
          |"plutoProjectTypeUuid": "330b4d84-ef24-41e2-b093-0d15829afa64",
          |"title": "projectlocker test 4",
          |"user": "andy_gallagher",
          |"workingGroupUuid": "1b97c363-fba0-4771-9cb5-9bd65aaed306",
          |"vidispineId":"VX-2345"}
        """.stripMargin

      val response = route(app, FakeRequest(
        method="PUT",
        uri="/api/project/external/create",
        headers=FakeHeaders(Seq(("Content-Type", "application/json"))),
        body=testCreateDocument).withSession("uid"->"testuser")).get
      val jsondata = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]

      status(response) must equalTo(CONFLICT)
      (jsondata \ "detail").as[String] mustEqual "projects already exist"
      val resultList = (jsondata \ "result").as[List[PlutoConflictReply]]
      resultList.length mustEqual 2
      resultList.head.project.vidispineProjectId must beSome("VX-2345")
      resultList.head.project.projectTitle mustEqual "ThatTestProject"
      resultList.head.project.id must beSome(3)
      resultList.head.possiblePlutoTypes.length mustEqual 1
      //test data does not have a working group set up
      resultList.head.possiblePlutoTypes.head.uuid mustEqual "330b4d84-ef24-41e2-b093-0d15829afa64"
      resultList(1).project.id must beSome(4)
      resultList(1).possiblePlutoTypes.length mustEqual 1
    }
  }

  "ProjectEntryController.getByVsid" should {
    "return a ProjectEntry instance based on vidispine ID" in new WithApplication(buildApp){
      implicit val system:ActorSystem = app.actorSystem
      implicit val materializer:ActorMaterializer = ActorMaterializer()
      //needed for database access
      private val injector = app.injector
      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val response = route(app,
        FakeRequest(GET,"/api/project/by-vsid/VX-1234").withSession("uid"->"testuser")
      ).get

      status(response) must equalTo(OK)
      val jsondata = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]
      jsondata.toString mustEqual """{"status":"ok","result":[{"id":2,"projectTypeId":1,"vidispineId":"VX-1234","title":"AnotherTestProject","created":"2016-12-11T12:21:11.021+0000","user":"you"}]}"""
    }

    "return 404 for an unkown vidispine ID" in new WithApplication(buildApp) {
      implicit val system:ActorSystem = app.actorSystem
      implicit val materializer:ActorMaterializer = ActorMaterializer()
      //needed for database access
      private val injector = app.injector
      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val response = route(app,
        FakeRequest(GET,"/api/project/by-vsid/VX-99999").withSession("uid"->"testuser")
      ).get

      status(response) must equalTo(NOT_FOUND)
    }
  }

  "ProjectEntryController.updateTitle" should {
    "update the title field of an existing record" in new WithApplication(buildApp){
      implicit val system:ActorSystem = app.actorSystem
      implicit val materializer:ActorMaterializer = ActorMaterializer()
      //needed for database access
      private val injector = app.injector
      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val testUpdateDocument =
        """{
          |  "title": "some new title",
          |  "vsid": null
          |}""".stripMargin

      val dbRecordBefore = Await.result(ProjectEntry.entryForId(1),5.seconds).get
      dbRecordBefore.projectTitle mustEqual "InitialTestProject"

      val response = route(app, FakeRequest(
        method="PUT",
        uri="/api/project/1/title",
        headers=FakeHeaders(Seq(("Content-Type","application/json"))),
        body=testUpdateDocument).withSession("uid"->"testuser")).get

      val jsondata = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]
      status(response) must equalTo(OK)
      (jsondata \ "status").as[String] must equalTo("ok")
      (jsondata \ "detail").as[String] must equalTo("1 record(s) updated")

      val dbRecordAfter = Await.result(ProjectEntry.entryForId(1),5.seconds).get
      dbRecordAfter.projectTitle mustEqual "some new title"
    }
    "return 404 for a record that does not exist" in new WithApplication(buildApp){
      implicit val system:ActorSystem = app.actorSystem
      implicit val materializer:ActorMaterializer = ActorMaterializer()
      //needed for database access
      private val injector = app.injector
      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val testUpdateDocument =
        """{
          |  "title": "some new title",
          |  "vsid": null
          |}""".stripMargin

      val response = route(app, FakeRequest(
        method="PUT",
        uri="/api/project/9999/title",
        headers=FakeHeaders(Seq(("Content-Type","application/json"))),
        body=testUpdateDocument).withSession("uid"->"testuser")).get

      status(response) must equalTo(NOT_FOUND)
    }
  }

  "ProjectEntryController.updateTitleByVsid" should {
    "update the title field of an existing record" in new WithApplication(buildApp) {
      implicit val system:ActorSystem = app.actorSystem
      implicit val materializer:ActorMaterializer = ActorMaterializer()
      //needed for database access
      private val injector = app.injector
      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val testUpdateDocument =
        """{
          |  "title": "some other new title",
          |  "vsid": null
          |}""".stripMargin

      val dbRecordBefore = Await.result(ProjectEntry.entryForId(2),5.seconds).get
      dbRecordBefore.projectTitle mustEqual "AnotherTestProject"

      val response = route(app, FakeRequest(
        method="PUT",
        uri="/api/project/by-vsid/VX-1234/title",
        headers=FakeHeaders(Seq(("Content-Type","application/json"))),
        body=testUpdateDocument).withSession("uid"->"testuser")).get

      val jsondata = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]
      status(response) must equalTo(OK)
      (jsondata \ "status").as[String] must equalTo("ok")
      (jsondata \ "detail").as[String] must equalTo("1 record(s) updated")

      val dbRecordAfter = Await.result(ProjectEntry.entryForId(2),5.seconds).get
      dbRecordAfter.projectTitle mustEqual "some other new title"
    }
    "update the title field of multiple matching records" in new WithApplication(buildApp){
      implicit val system:ActorSystem = app.actorSystem
      implicit val materializer:ActorMaterializer = ActorMaterializer()
      //needed for database access
      private val injector = app.injector
      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val testUpdateDocument =
        """{
          |  "title": "ThatTestProject",
          |  "vsid": null
          |}""".stripMargin

      val dbRecordBefore = Await.result(ProjectEntry.entryForId(3),5.seconds).get
      dbRecordBefore.projectTitle mustEqual "ThatTestProject"

      val response = route(app, FakeRequest(
        method="PUT",
        uri="/api/project/by-vsid/VX-2345/title",
        headers=FakeHeaders(Seq(("Content-Type","application/json"))),
        body=testUpdateDocument).withSession("uid"->"testuser")).get

      val jsondata = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]
      println(jsondata.toString())
      status(response) must equalTo(OK)
      (jsondata \ "status").as[String] must equalTo("ok")
      (jsondata \ "detail").as[String] must equalTo("2 record(s) updated")

      val dbRecordAfter = Await.result(ProjectEntry.entryForId(3),5.seconds).get
      dbRecordAfter.projectTitle mustEqual "ThatTestProject"
      val dbRecordAfterNext = Await.result(ProjectEntry.entryForId(4),5.seconds).get
      dbRecordAfterNext.projectTitle mustEqual "ThatTestProject"
    }

    "return 404 for a record that does not exist" in new WithApplication(buildApp){
      implicit val system:ActorSystem = app.actorSystem
      implicit val materializer:ActorMaterializer = ActorMaterializer()
      //needed for database access
      private val injector = app.injector
      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val testUpdateDocument =
        """{
          |  "title": "some new title",
          |  "vsid": null
          |}""".stripMargin

      val response = route(app, FakeRequest(
        method="PUT",
        uri="/api/project/by-vsid/VX-99999/title",
        headers=FakeHeaders(Seq(("Content-Type","application/json"))),
        body=testUpdateDocument).withSession("uid"->"testuser")).get

      val jsondata = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]
      println(jsondata.toString)
      status(response) must equalTo(NOT_FOUND)
    }
  }

  "ProjectEntryController.updateVsid" should {
    "update the vidipsineProjectId field of an existing record" in new WithApplication(buildApp) {
      implicit val system:ActorSystem = app.actorSystem
      implicit val materializer:ActorMaterializer = ActorMaterializer()
      //needed for database access
      private val injector = app.injector
      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val testUpdateDocument =
        """{
          |  "title": "",
          |  "vsid": "VX-5678"
          |}""".stripMargin

      val dbRecordBefore = Await.result(ProjectEntry.entryForId(1),5.seconds).get
      dbRecordBefore.vidispineProjectId must beNone

      val response = route(app, FakeRequest(
        method="PUT",
        uri="/api/project/1/vsid",
        headers=FakeHeaders(Seq(("Content-Type","application/json"))),
        body=testUpdateDocument).withSession("uid"->"testuser")).get

      val jsondata = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]
      status(response) must equalTo(OK)
      (jsondata \ "status").as[String] must equalTo("ok")
      (jsondata \ "detail").as[String] must equalTo("1 record(s) updated")

      val dbRecordAfter = Await.result(ProjectEntry.entryForId(1),5.seconds).get
      dbRecordAfter.vidispineProjectId must beSome("VX-5678")
    }

    "return 404 for a record that does not exist" in new WithApplication(buildApp){
      implicit val system:ActorSystem = app.actorSystem
      implicit val materializer:ActorMaterializer = ActorMaterializer()
      //needed for database access
      private val injector = app.injector
      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val testUpdateDocument =
        """{
          |  "title": "",
          |  "vsid": "VX-5678"
          |}""".stripMargin

      val response = route(app, FakeRequest(
        method="PUT",
        uri="/api/project/9999/vsid",
        headers=FakeHeaders(Seq(("Content-Type","application/json"))),
        body=testUpdateDocument).withSession("uid"->"testuser")).get

      status(response) must equalTo(NOT_FOUND)
    }
  }

  "ProjectEntryController.listFiltered" should {
    "show projectentry items filtered by title" in new WithApplication(buildApp){
      implicit val system:ActorSystem = app.actorSystem
      implicit val materializer:ActorMaterializer = ActorMaterializer()
      //needed for database access
      private val injector = app.injector
      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val testSearchDocument =
        """{
          |  "title": "ThatTestProject",
          |  "match": "W_ENDSWITH"
          |}""".stripMargin

      val response = route(app, FakeRequest(
        method="PUT",
        uri="/api/project/list",
        headers=FakeHeaders(Seq(("Content-Type","application/json"))),
        body=testSearchDocument).withSession("uid"->"testuser")).get

      val jsondata = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]
      println(jsondata.toString())
      status(response) must equalTo(OK)

      val resultList = (jsondata \ "result").as[List[ProjectEntry]]
      resultList.length mustEqual 2
      resultList.head.id must beSome(3)
      resultList(1).id must beSome(4)
    }

    "show projectentry items filtered by vidispine id" in new WithApplication(buildApp){
      implicit val system:ActorSystem = app.actorSystem
      implicit val materializer:ActorMaterializer = ActorMaterializer()
      //needed for database access
      private val injector = app.injector
      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val testSearchDocument =
        """{
          |  "vidispineId": "VX-2345",
          |  "match": "W_ENDSWITH"
          |}""".stripMargin

      val response = route(app, FakeRequest(
        method="PUT",
        uri="/api/project/list",
        headers=FakeHeaders(Seq(("Content-Type","application/json"))),
        body=testSearchDocument).withSession("uid"->"testuser")).get

      val jsondata = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]
      status(response) must equalTo(OK)

      val resultList = (jsondata \ "result").as[List[ProjectEntry]]
      resultList.length mustEqual 2
      resultList.head.id must beSome(3)
      resultList(1).id must beSome(4)
    }

    "return an empty list for a filename that is not associated with any projects" in new WithApplication(buildApp){
      implicit val system:ActorSystem = app.actorSystem
      implicit val materializer:ActorMaterializer = ActorMaterializer()
      //needed for database access
      private val injector = app.injector
      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val testSearchDocument =
        """{
          |  "match": "W_ENDSWITH",
          |  "filename": "/path/to/another/file.project"
          |}""".stripMargin

      val response = route(app, FakeRequest(
        method="PUT",
        uri="/api/project/list",
        headers=FakeHeaders(Seq(("Content-Type","application/json"))),
        body=testSearchDocument).withSession("uid"->"testuser")).get

      val jsondata = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]
      println(jsondata.toString)
      status(response) must equalTo(OK)

      val resultList = (jsondata \ "result").as[List[ProjectEntry]]
      resultList.length mustEqual 0
    }

    "show projectentry items filtered by file association" in new WithApplication(buildApp){
      implicit val system:ActorSystem = app.actorSystem
      implicit val materializer:ActorMaterializer = ActorMaterializer()
      //needed for database access
      private val injector = app.injector
      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val testSearchDocument =
        """{
          |  "match": "W_ENDSWITH",
          |  "filename": "/path/to/thattestproject"
          |}""".stripMargin

      val response = route(app, FakeRequest(
        method="PUT",
        uri="/api/project/list",
        headers=FakeHeaders(Seq(("Content-Type","application/json"))),
        body=testSearchDocument).withSession("uid"->"testuser")).get

      val jsondata = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]
      println(jsondata.toString)
      status(response) must equalTo(OK)

      val resultList = (jsondata \ "result").as[List[ProjectEntry]]
      resultList.length mustEqual 1
      resultList.head.id must beSome(3)
      resultList.head.projectTitle mustEqual "ThatTestProject"
    }

    "return an empty list if nothing matches" in new WithApplication(buildApp){
      implicit val system:ActorSystem = app.actorSystem
      implicit val materializer:ActorMaterializer = ActorMaterializer()
      //needed for database access
      private val injector = app.injector
      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val testSearchDocument =
        """{
          |  "match": "W_ENDSWITH",
          |  "title": "Fdgfsdgfgdsfgdsfgsd"
          |}""".stripMargin

      val response = route(app, FakeRequest(
        method="PUT",
        uri="/api/project/list",
        headers=FakeHeaders(Seq(("Content-Type","application/json"))),
        body=testSearchDocument).withSession("uid"->"testuser")).get

      val jsondata = Await.result(bodyAsJsonFuture(response), 5.seconds).as[JsValue]
      status(response) must equalTo(OK)

      val resultList = (jsondata \ "result").as[List[ProjectEntry]]
      resultList.length mustEqual 0
    }

    "reject invalid json doc with 400" in new WithApplication(buildApp){
      implicit val system:ActorSystem = app.actorSystem
      implicit val materializer:ActorMaterializer = ActorMaterializer()
      //needed for database access
      private val injector = app.injector
      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val testSearchDocument =
        """{
          |  "title": "ThatTestProject",
          |}""".stripMargin

      val response = route(app, FakeRequest(
        method="PUT",
        uri="/api/project/list",
        headers=FakeHeaders(Seq(("Content-Type","application/json"))),
        body=testSearchDocument).withSession("uid"->"testuser")).get

      status(response) must equalTo(BAD_REQUEST)
    }
  }
}
