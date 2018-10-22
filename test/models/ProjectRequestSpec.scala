package models

import org.specs2.mutable.Specification
import models._
import play.api.db.slick.DatabaseConfigProvider
import play.api.test.WithApplication
import slick.jdbc.{JdbcProfile, PostgresProfile}
import utils.BuildMyApp

import scala.concurrent.Await
import scala.concurrent.duration._

class ProjectRequestSpec extends Specification with BuildMyApp {
  "ProjectRequest.hydrate" should {
    "return Some(ProjectRequestFull) when provided with existing IDs" in new WithApplication(buildApp){
      private val injector = app.injector

      protected val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      protected implicit val db = dbConfigProvider.get[PostgresProfile].db

      val pr = ProjectRequest("testfile",1,"MyTestProject", 1,"testuser",None,None,deletable=true, deep_archive=false, sensitive=false)

      val testStorage = Await.result(StorageEntryHelper.entryFor(1),10.seconds)
      val testTemplate = Await.result(ProjectTemplate.entryFor(1),10.seconds)

      val result:Option[ProjectRequestFull] = Await.result(pr.hydrate,10.seconds)

      result must beSome(ProjectRequestFull("testfile",testStorage.get,"MyTestProject", testTemplate.get,"testuser",None,None, existingVidispineId = None, shouldNotify=true,deletable=true, deep_archive=false, sensitive=false))
    }

    "return None if provided storage ID is invalid" in new WithApplication(buildApp){
      private val injector = app.injector

      protected val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      protected implicit val db = dbConfigProvider.get[PostgresProfile].db

      val pr = ProjectRequest("testfile",999999,"MyTestProject",1,"testuser",None,None,deletable=true, deep_archive=false, sensitive=false)

      val result:Option[ProjectRequestFull] = Await.result(pr.hydrate,10.seconds)

      result must beNone
    }

    "return None if provided template ID is invalid" in new WithApplication(buildApp){
      private val injector = app.injector

      protected val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      protected implicit val db = dbConfigProvider.get[PostgresProfile].db

      val pr = ProjectRequest("testfile",1,"MyTestProject",999999,"testuser",None,None,deletable=true, deep_archive=false, sensitive=false)

      val result:Option[ProjectRequestFull] = Await.result(pr.hydrate,10.seconds)

      result must beNone
    }

    "return None if all provided IDs are invalid" in new WithApplication(buildApp){
      private val injector = app.injector

      protected val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      protected implicit val db = dbConfigProvider.get[PostgresProfile].db

      val pr = ProjectRequest("testfile",999999,"MyTestProject",999999,"testuser",None,None,deletable=true, deep_archive=false, sensitive=false)

      val result:Option[ProjectRequestFull] = Await.result(pr.hydrate,10.seconds)

      result must beNone
    }
  }
}
