import java.io.IOError
import java.sql.Timestamp
import java.time.{Instant, LocalDateTime, ZonedDateTime}

import akka.actor.ActorSystem
import helpers._
import models.{PostrunAction, _}

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
import akka.testkit.{ ImplicitSender, TestActors, TestKit }

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
  private val actorSystem = injector.instanceOf(classOf[ActorSystem])
  private implicit val db = dbConfigProvider.get[JdbcProfile].db

  private implicit val config:Configuration = Configuration.from(Map(
    "postrun.timeout"->"30 seconds",
    "postrun.scriptsPath"->"postrun/test_scripts"
  ))

  "ProjectCreateHelper.create" should {
    "create a saved ProjectEntry in response to a valid request" in {
      val mockedStorageHelper = mock[StorageHelper]
      val mockedActor = actorSystem.actorOf(TestActors.echoActorProps)

      //assume that the copyFile operation works, this is tested elsewhere
      mockedStorageHelper.copyFile(any[FileEntry],any[FileEntry])(any[JdbcBackend#DatabaseDef]) answers((paramArray,mock)=>{
        val parameters = paramArray.asInstanceOf[Array[Object]]
        Future(Right(parameters(1).asInstanceOf[FileEntry]))
      })

      val p = new ProjectCreateHelperImpl(mockedActor, config, dbConfigProvider) { override protected val storageHelper:StorageHelper=mockedStorageHelper }

      val request = ProjectRequest("testfile",1,"MyTestProjectFile", 3,"test-user",None,None).hydrate

      val fullRequest = Await.result(request, 10.seconds)
      fullRequest must beSome

      val createTime=LocalDateTime.now()

      val response = p.create(fullRequest.get, Some(createTime))
      val createResult = Await.result(response, 10.seconds)

      createResult must beSuccessfulTry(ProjectEntry(Some(5),2,None,"MyTestProjectFile",Timestamp.valueOf(createTime),"test-user",None,None,None))
    }

    "refuse to overwrite an existing file that has data on it" in {
      val mockedStorageHelper = mock[StorageHelper]
      val mockedActor = actorSystem.actorOf(TestActors.echoActorProps)

      //assume that the copyFile operation works, this is tested elsewhere
      mockedStorageHelper.copyFile(any[FileEntry],any[FileEntry])(any[JdbcBackend#DatabaseDef]) answers((paramArray,mock)=>{
        val parameters = paramArray.asInstanceOf[Array[Object]]
        Future(Right(parameters(1).asInstanceOf[FileEntry]))
      })

      val p = new ProjectCreateHelperImpl(mockedActor, config, dbConfigProvider) { override protected val storageHelper:StorageHelper=mockedStorageHelper }

      val request = ProjectRequest("/path/to/a/file.project",1,"MyTestProjectFile", 1,"test-user",None,None).hydrate

      val fullRequest = Await.result(request, 10.seconds)

      val createTime=LocalDateTime.now()

      val response = p.create(fullRequest.get, Some(createTime))
      val createResult = Await.result(response, 10.seconds)

      createResult must beFailedTry
      createResult.failed.get.toString mustEqual "exceptions.ProjectCreationError: File /path/to/a/file.project on Local (no root path) (no host) already has data"
    }

    "return an error in response to an invalid request" in {
      val mockedStorageHelper = mock[StorageHelper]
      val mockedActor = actorSystem.actorOf(TestActors.echoActorProps)

      //assume that the copyFile operation works, this is tested elsewhere
      mockedStorageHelper.copyFile(any[FileEntry],any[FileEntry])(any[JdbcBackend#DatabaseDef]) answers((paramArray,mock)=>{
        val parameters = paramArray.asInstanceOf[Array[Object]]
        Future(Right(parameters(1).asInstanceOf[FileEntry]))
      })

      val p = new ProjectCreateHelperImpl(mockedActor, config, dbConfigProvider) { override protected val storageHelper:StorageHelper=mockedStorageHelper }

      val request = ProjectRequest("testfile",2,"MyTestProjectFile",1,"test-user",None,None).hydrate

      val fullRequest = Await.result(request, 10.seconds)
      val createTime=LocalDateTime.now()

      val response = p.create(fullRequest.get, Some(createTime))
      val createResult = Await.result(response, 10.seconds)

      createResult must beFailedTry
    }
  }

  "ProjectCreateHelper.runNextAction" should {
    "call the run method of the postrun action entry and wait for result" in {
      val pretendProjectName = "/tmp/pretendproject"
      Seq("/bin/dd","if=/dev/urandom",s"of=$pretendProjectName","bs=1k","count=600").!
      val mockedActor = actorSystem.actorOf(TestActors.echoActorProps)

      val p = new ProjectCreateHelperImpl(mockedActor, config, dbConfigProvider)
      implicit val scriptTimeout = 5.seconds
      val testTimestamp = Timestamp.valueOf("2018-02-02 03:04:05")
      val testPostrunAction = PostrunAction(None,"args_test_4.py","Test script",None,"testuser",1,testTimestamp)
      val testProjectEntry = ProjectEntry(None,1,None,"Test project title",testTimestamp, "testuser",None,None,None)
      val testProjectType = ProjectType(None,"TestProject","TestProjectApp","1.0",None,None)
      val testWorkingGroup = PlutoWorkingGroup(None,None,"Test working group","230C365E-9F88-42BC-87AB-B88A3F0B8101")
      val testCommission = PlutoCommission(None,234,"VX",testTimestamp, testTimestamp, "Test commission","New",None,1)

      val result = p.runNextAction(actions=Seq(testPostrunAction),
        results=Seq(),
        cache=PostrunDataCache(Map("key_one"->"value_one","key_two"->"value_two")),
        projectFileName="/tmp/pretendproject",
        projectEntry=testProjectEntry,
        projectType=testProjectType,
        workingGroupMaybe = Some(testWorkingGroup),
        commissionMaybe = Some(testCommission))

      result.head must beSuccessfulTry
      result.head.get.raisedError must beNone
      //FIXME: this assertion does not work as the ordering can change - need to think of a better way of doing this
      //result.head.get.stdOutContents mustEqual "I was provided with {'projectFile': '/tmp/pretendproject', 'commissionId': 'VX-234', 'vidispineProjectId': '', 'workingGroupName': 'Test working group', 'commissionDescription': '', 'projectTypeId': '', 'projectTypeName': 'TestProject', 'projectFileExtension': '', 'projectCreated': '2018-02-02 03:04:05.0', 'projectOwner': 'testuser', 'commissionCreated': '2018-02-02 03:04:05.0', 'projectTargetVersion': '1.0', 'workingGroupUuid': '230C365E-9F88-42BC-87AB-B88A3F0B8101', 'projectOpensWith': 'TestProjectApp', 'dataCache': {'key_two': 'value_two', 'key_one': 'value_one'}, 'projectId': '', 'commissionTitle': 'Test commission', 'workingGroupHide': '', 'projectTitle': 'Test project title'}\n"
    }

  }

  "PostrunCreateHelper.doPostrunActions" should {
    "execute all postrun actions for a project type" in {
      val mockedActor = actorSystem.actorOf(TestActors.echoActorProps)

      val p = new ProjectCreateHelperImpl(mockedActor, config, dbConfigProvider) {
        override protected def syncExecScript(action: PostrunAction, projectFileName: String, entry: ProjectEntry, projectType: ProjectType, cache: PostrunDataCache, workingGroupMaybe: Option[PlutoWorkingGroup], commissionMaybe: Option[PlutoCommission])(implicit db: slick.jdbc.JdbcProfile#Backend#Database, config:play.api.Configuration, timeout: Duration) : Try[JythonOutput] =
          Success(JythonOutput("this worked","",cache,None))
      }

      val testTimestamp = Timestamp.valueOf("2018-02-02 03:04:05")
      val testFileEntry = Await.result(FileEntry.entryFor("/path/to/a/file.project",1),5.seconds).get.head
      val testProjectEntry = ProjectEntry(None,1,None,"Test project title",testTimestamp, "testuser",None,None,None)
      val testProjectTemplate = Await.result(ProjectTemplate.entryFor(1),5.seconds).get

      val result = Await.result(p.doPostrunActions(testFileEntry,testProjectEntry,testProjectTemplate),5.seconds)
      result must beRight("Successfully ran 2 postrun actions for project /path/to/a/file.project")
    }

    "indicate a failure if any postrun action failed" in {
      val mockedActor = actorSystem.actorOf(TestActors.echoActorProps)

      val p = new ProjectCreateHelperImpl(mockedActor, config, dbConfigProvider) {
        override protected def syncExecScript(action: PostrunAction, projectFileName: String, entry: ProjectEntry, projectType: ProjectType, cache: PostrunDataCache, workingGroupMaybe: Option[PlutoWorkingGroup], commissionMaybe: Option[PlutoCommission])(implicit db: slick.jdbc.JdbcProfile#Backend#Database, config:play.api.Configuration, timeout: Duration) : Try[JythonOutput] =
          if (action.id.get == 1)
          //this is normally mapped into a failure by models.PostrunAction.run, and detailed debug output to the log there too.
            Failure(new RuntimeException("my hovercraft is full of eels"))
          else
            Success(JythonOutput("this worked", "",cache, None))
      }

      val testTimestamp = Timestamp.valueOf("2018-02-02 03:04:05")
      val testFileEntry = Await.result(FileEntry.entryFor("/path/to/a/file.project",1),5.seconds).get.head
      val testProjectEntry = ProjectEntry(None,1,None,"Test project title",testTimestamp, "testuser",None,None,None)
      val testProjectTemplate = Await.result(ProjectTemplate.entryFor(1),5.seconds).get

      val result = Await.result(p.doPostrunActions(testFileEntry,testProjectEntry,testProjectTemplate),5.seconds)
      result must beLeft("1 postrun actions failed for project /path/to/a/file.project, see log for details")
    }
  }

  "ProjectCreateHelper.orderPostruns" should {
    "take an unordered set of postruns and dependency information and produce an ordered set of postruns" in {
      val mockedActor = actorSystem.actorOf(TestActors.echoActorProps)
      val postrunSet = Seq(
        PostrunAction(Some(1),"first_test.py","First test depends on 5 and 6",None,"test",1,Timestamp.valueOf(LocalDateTime.now())),
          PostrunAction(Some(2),"second_test.py","Second test depends on 1",None,"test",1,Timestamp.valueOf(LocalDateTime.now())),
          PostrunAction(Some(3),"third_test.py","Third test depends on nothing",None,"test",1,Timestamp.valueOf(LocalDateTime.now())),
          PostrunAction(Some(4),"fourth_test.py","Fourth test depends on 3",None,"test",1,Timestamp.valueOf(LocalDateTime.now())),
          PostrunAction(Some(5),"fifth_test.py","Fifth test depends on 7 and 4",None,"test",1,Timestamp.valueOf(LocalDateTime.now())),
          PostrunAction(Some(6),"sixth_test.py","Sixth test depends on nothing",None,"test",1,Timestamp.valueOf(LocalDateTime.now())),
          PostrunAction(Some(7),"seventh_test.py","Seventh test depends on nothing",None,"test",1,Timestamp.valueOf(LocalDateTime.now()))
      )

      val dependencyGraph = Map(
        1->Seq(5,6),
        2->Seq(1),
        5->Seq(7,4),
        4->Seq(3)
      )

      val p = new ProjectCreateHelperImpl(mockedActor, config, dbConfigProvider)
      val result = p.orderPostruns(postrunSet, dependencyGraph)
      //keeping below line as it's handy for debugging!
      //result.foreach(action=>println(action.title))

      val idSeq = result.map(action=>action.id.get)
      idSeq mustEqual Seq(7,6,3,4,5,1,2)
    }
  }
}
