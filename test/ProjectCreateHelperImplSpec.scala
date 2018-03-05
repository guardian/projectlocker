import java.io.IOError
import java.sql.Timestamp
import java.time.{Instant, LocalDateTime}

import helpers._
import models._

import sys.process._
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.mock.Mockito
import org.specs2.runner.JUnitRunner
import play.api.Configuration
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import slick.jdbc.{JdbcBackend, JdbcProfile}
import testHelpers.TestDatabase

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

@RunWith(classOf[JUnitRunner])
class ProjectCreateHelperImplSpec extends Specification with Mockito {
  sequential

  //can over-ride bindings here. see https://www.playframework.com/documentation/2.5.x/ScalaTestingWithGuice
  private val application = new GuiceApplicationBuilder()
    .overrides(bind[DatabaseConfigProvider].to[TestDatabase.testDbProvider])
    .build
  private val injector = application.injector

  private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
  private implicit val db = dbConfigProvider.get[JdbcProfile].db

  private implicit val config:Configuration = Configuration.from(Map(
    "postrun.timeout"->"10 seconds",
    "postrun.scriptsPath"->"postrun/test_scripts"
  ))

  "ProjectCreateHelper.create" should {
    "create a saved ProjectEntry in response to a valid request" in {
      val mockedStorageHelper = mock[StorageHelper]

      //assume that the copyFile operation works, this is tested elsewhere
      mockedStorageHelper.copyFile(any[FileEntry],any[FileEntry])(any[JdbcBackend#DatabaseDef]) answers((paramArray,mock)=>{
        val parameters = paramArray.asInstanceOf[Array[Object]]
        Future(Right(parameters(1).asInstanceOf[FileEntry]))
      })

      val p = new ProjectCreateHelperImpl { override protected val storageHelper:StorageHelper=mockedStorageHelper }

      val request = ProjectRequest("testfile",1,"MyTestProjectFile", 2,"test-user").hydrate

      val fullRequest = Await.result(request, 10.seconds)

      val createTime=LocalDateTime.now()

      println(s"create time is $createTime")
      val response = p.create(fullRequest.get, Some(createTime))
      val createResult = Await.result(response, 10.seconds)

      createResult must beSuccessfulTry(ProjectEntry(Some(5),2,None,"MyTestProjectFile",Timestamp.valueOf(createTime),"test-user"))
    }

    "refuse to overwrite an existing file that has data on it" in {
      val mockedStorageHelper = mock[StorageHelper]

      //assume that the copyFile operation works, this is tested elsewhere
      mockedStorageHelper.copyFile(any[FileEntry],any[FileEntry])(any[JdbcBackend#DatabaseDef]) answers((paramArray,mock)=>{
        val parameters = paramArray.asInstanceOf[Array[Object]]
        Future(Right(parameters(1).asInstanceOf[FileEntry]))
      })

      val p = new ProjectCreateHelperImpl { override protected val storageHelper:StorageHelper=mockedStorageHelper }

      val request = ProjectRequest("/path/to/a/file.project",1,"MyTestProjectFile", 1,"test-user").hydrate

      val fullRequest = Await.result(request, 10.seconds)

      val createTime=LocalDateTime.now()

      val response = p.create(fullRequest.get, Some(createTime))
      val createResult = Await.result(response, 10.seconds)

      createResult must beFailedTry
      createResult.failed.get.toString mustEqual "exceptions.ProjectCreationError: File /path/to/a/file.project on Local (no root path) (no host) already has data"
    }

    "return an error in response to an invalid request" in {
      val mockedStorageHelper = mock[StorageHelper]

      //assume that the copyFile operation works, this is tested elsewhere
      mockedStorageHelper.copyFile(any[FileEntry],any[FileEntry])(any[JdbcBackend#DatabaseDef]) answers((paramArray,mock)=>{
        val parameters = paramArray.asInstanceOf[Array[Object]]
        Future(Right(parameters(1).asInstanceOf[FileEntry]))
      })

      val p = new ProjectCreateHelperImpl { override protected val storageHelper:StorageHelper=mockedStorageHelper }

      val request = ProjectRequest("testfile",2,"MyTestProjectFile",1,"test-user").hydrate

      val fullRequest = Await.result(request, 10.seconds)
      val createTime=LocalDateTime.now()

      println(s"create time is $createTime")
      val response = p.create(fullRequest.get, Some(createTime))
      val createResult = Await.result(response, 10.seconds)

      createResult must beFailedTry
    }
  }

  "ProjectCreateHelper.runEach" should {
    "call the run method of the postrun action entry and wait for result" in {
      val pretendProjectName = "/tmp/pretendproject"
      Seq("/bin/dd","if=/dev/urandom",s"of=$pretendProjectName","bs=1k","count=600").!

      val p = new ProjectCreateHelperImpl {
        def testRunEach(action:PostrunAction, projectFileName:String, projectEntry:ProjectEntry,projectType:ProjectType)
                       (implicit db: slick.jdbc.JdbcProfile#Backend#Database, config:play.api.Configuration) =
          runEach(action,projectFileName,projectEntry, projectType)(db, config)
      }

      val testTimestamp = Timestamp.valueOf("2018-02-02 03:04:05")
      val testPostrunAction = PostrunAction(None,"args_test_4.py","Test script",None,"testuser",1,testTimestamp)
      val testProjectEntry = ProjectEntry(None,1,None,"Test project title",testTimestamp, "testuser")
      val testProjectType = ProjectType(None,"TestProject","TestProjectApp","1.0",None)

      val result = p.testRunEach(testPostrunAction,pretendProjectName,testProjectEntry,testProjectType)
      result must beSuccessfulTry
      println(result.get.stdOutContents)
      result.get.raisedError must beNone
      result.get.stdOutContents mustEqual "I was provided with {'projectFile': '/tmp/pretendproject', 'vidispineProjectId': '', 'projectTypeId': '', 'projectTypeName': 'TestProject', 'projectFileExtension': '', 'projectCreated': '2018-02-02 03:04:05.0', 'projectOwner': 'testuser', 'projectTargetVersion': '1.0', 'projectOpensWith': 'TestProjectApp', 'projectId': '', 'projectTitle': 'Test project title'}\n"
    }

    "return a Failure if the postrun action script can't be found" in {
      val pretendProjectName = "/tmp/pretendproject"
      Seq("/bin/dd","if=/dev/urandom",s"of=$pretendProjectName","bs=1k","count=600").!

      val p = new ProjectCreateHelperImpl {
        def testRunEach(action:PostrunAction, projectFileName:String, projectEntry:ProjectEntry,projectType:ProjectType)
                       (implicit db: slick.jdbc.JdbcProfile#Backend#Database, config:play.api.Configuration) =
          runEach(action,projectFileName,projectEntry, projectType)(db, config)
      }

      val testTimestamp = Timestamp.valueOf("2018-02-02 03:04:05")
      val testPostrunAction = PostrunAction(None,"notexistingscript.py","Test script",None,"testuser",1,testTimestamp)
      val testProjectEntry = ProjectEntry(None,1,None,"Test project title",testTimestamp, "testuser")
      val testProjectType = ProjectType(None,"TestProject","TestProjectApp","1.0",None)

      val result = p.testRunEach(testPostrunAction,pretendProjectName,testProjectEntry,testProjectType)
      result must beFailedTry
      result.failed.get.toString must contain("No such file or directory")
    }

    "return a Failure if the script errored and provide exception details" in {
      val pretendProjectName = "/tmp/pretendproject"
      Seq("/bin/dd","if=/dev/urandom",s"of=$pretendProjectName","bs=1k","count=600").!

      val p = new ProjectCreateHelperImpl {
        def testRunEach(action:PostrunAction, projectFileName:String, projectEntry:ProjectEntry,projectType:ProjectType)
                       (implicit db: slick.jdbc.JdbcProfile#Backend#Database, config:play.api.Configuration) =
          runEach(action,projectFileName,projectEntry, projectType)(db, config)
      }

      val testTimestamp = Timestamp.valueOf("2018-02-02 03:04:05")
      val testPostrunAction = PostrunAction(None,"error_test_2.py","Test script",None,"testuser",1,testTimestamp)
      val testProjectEntry = ProjectEntry(None,1,None,"Test project title",testTimestamp, "testuser")
      val testProjectType = ProjectType(None,"TestProject","TestProjectApp","1.0",None)

      val result = p.testRunEach(testPostrunAction,pretendProjectName,testProjectEntry,testProjectType)
      result must beFailedTry
      result.failed.get.toString must contain("StandardError(\"My hovercraft is full of eels\")")
    }
  }

  "PostrunCreateHelper.doPostrunActions" should {
    "execute all postrun actions for a project type" in {
      val p = new ProjectCreateHelperImpl {
        override protected def runEach(action: PostrunAction, projectFileName: String, projectEntry: ProjectEntry, projectType: ProjectType)
                                      (implicit db: JdbcBackend#DatabaseDef, config: Configuration): Try[JythonOutput] =
          Success(JythonOutput("this worked","",None))
      }

      val testTimestamp = Timestamp.valueOf("2018-02-02 03:04:05")
      val testFileEntry = Await.result(FileEntry.entryFor("/path/to/a/file.project",1),5.seconds).get.head
      val testProjectEntry = ProjectEntry(None,1,None,"Test project title",testTimestamp, "testuser")
      val testProjectTemplate = Await.result(ProjectTemplate.entryFor(1),5.seconds).get

      val result = Await.result(p.doPostrunActions(testFileEntry,Future(Success(testProjectEntry)),testProjectTemplate),5.seconds)
      result must beRight("Successfully ran 2 postrun actions for project /path/to/a/file.project")
    }

    "indicate a failure if any postrun action failed" in {
      val p = new ProjectCreateHelperImpl {
        override protected def runEach(action: PostrunAction, projectFileName: String, projectEntry: ProjectEntry, projectType: ProjectType)
                                      (implicit db: JdbcBackend#DatabaseDef, config: Configuration): Try[JythonOutput] = {
          println(s"runEach: action id ${action.id.get}")
          if (action.id.get == 1)
            //this is normally mapped into a failure by models.PostrunAction.run, and detailed debug output to the log there too.
            Failure(new RuntimeException("my hovercraft is full of eels"))
          else
            Success(JythonOutput("this worked", "", None))
        }
      }

      val testTimestamp = Timestamp.valueOf("2018-02-02 03:04:05")
      val testFileEntry = Await.result(FileEntry.entryFor("/path/to/a/file.project",1),5.seconds).get.head
      val testProjectEntry = ProjectEntry(None,1,None,"Test project title",testTimestamp, "testuser")
      val testProjectTemplate = Await.result(ProjectTemplate.entryFor(1),5.seconds).get

      val result = Await.result(p.doPostrunActions(testFileEntry,Future(Success(testProjectEntry)),testProjectTemplate),5.seconds)
      result must beLeft("1 postrun actions failed for project /path/to/a/file.project, see log for details")
    }
  }
}
