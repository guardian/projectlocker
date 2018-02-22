import java.sql.Timestamp
import java.time.{Instant, LocalDateTime}

import org.mockito._
import helpers._
import models.{FileEntry, ProjectEntry, ProjectRequest}
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.mock.Mockito
import org.specs2.runner.JUnitRunner
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import slick.jdbc.{JdbcBackend, JdbcProfile}
import testHelpers.TestDatabase

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[JUnitRunner])
class ProjectCreateHelperImplSpec extends Specification with Mockito {
  //can over-ride bindings here. see https://www.playframework.com/documentation/2.5.x/ScalaTestingWithGuice
  private val application = new GuiceApplicationBuilder()
    .overrides(bind[DatabaseConfigProvider].to[TestDatabase.testDbProvider])
    .build
  private val injector = application.injector

  private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
  private implicit val db = dbConfigProvider.get[JdbcProfile].db

  "ProjectCreateHelper.create" should {
    "create a saved ProjectEntry in response to a valid request" in {
      val mockedStorageHelper = mock[StorageHelper]

      //assume that the copyFile operation works, this is tested elsewhere
      mockedStorageHelper.copyFile(any[FileEntry],any[FileEntry])(any[JdbcBackend#DatabaseDef]) answers((paramArray,mock)=>{
        val parameters = paramArray.asInstanceOf[Array[Object]]
        Future(Right(parameters(1).asInstanceOf[FileEntry]))
      })

      val p = new ProjectCreateHelperImpl { override protected val storageHelper:StorageHelper=mockedStorageHelper }

      val request = ProjectRequest("testfile",1,"MyTestProjectFile", 1,"test-user").hydrate

      val fullRequest = Await.result(request, 10.seconds)

      val createTime=LocalDateTime.now()

      println(s"create time is $createTime")
      val response = p.create(fullRequest.get, Some(createTime))
      val createResult = Await.result(response, 10.seconds)

      createResult must beSuccessfulTry(ProjectEntry(Some(2),1,None,"MyTestProjectFile",Timestamp.valueOf(createTime),"test-user"))
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
}
