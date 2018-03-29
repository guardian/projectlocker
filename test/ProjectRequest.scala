import org.specs2.mutable.Specification
import models._
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import slick.jdbc.JdbcProfile
import testHelpers.TestDatabase

import scala.concurrent.Await
import scala.concurrent.duration._

class ProjectRequest extends Specification {
  //can over-ride bindings here. see https://www.playframework.com/documentation/2.5.x/ScalaTestingWithGuice
  private val application = new GuiceApplicationBuilder()
    .overrides(bind[DatabaseConfigProvider].to[TestDatabase.testDbProvider])
    .build
  private val injector = application.injector

  private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
  private implicit val db = dbConfigProvider.get[JdbcProfile].db

  "ProjectRequest.hydrate" should {
    "return Some(ProjectRequestFull) when provided with existing IDs" in {
      val pr = ProjectRequest("testfile",1,"MyTestProject", 1,"testuser",None,None)

      val testStorage = Await.result(StorageEntryHelper.entryFor(1),10.seconds)
      val testTemplate = Await.result(ProjectTemplate.entryFor(1),10.seconds)

      val result:Option[ProjectRequestFull] = Await.result(pr.hydrate,10.seconds)

      result must beSome(ProjectRequestFull("testfile",testStorage.get,"MyTestProject", testTemplate.get,"testuser",None,None, shouldNotify=true))
    }

    "return None if provided storage ID is invalid" in {
      val pr = ProjectRequest("testfile",999999,"MyTestProject",1,"testuser",None,None)

      val result:Option[ProjectRequestFull] = Await.result(pr.hydrate,10.seconds)

      result must beNone
    }

    "return None if provided template ID is invalid" in {
      val pr = ProjectRequest("testfile",1,"MyTestProject",999999,"testuser",None,None)

      val result:Option[ProjectRequestFull] = Await.result(pr.hydrate,10.seconds)

      result must beNone
    }

    "return None if all provided IDs are invalid" in {
      val pr = ProjectRequest("testfile",999999,"MyTestProject",999999,"testuser",None,None)

      val result:Option[ProjectRequestFull] = Await.result(pr.hydrate,10.seconds)

      result must beNone
    }
  }
}
