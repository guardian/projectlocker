package models

import org.specs2.mutable.Specification
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.WithApplication
import slick.jdbc.{JdbcProfile, PostgresProfile}
import testHelpers.TestDatabase
import utils.BuildMyApp

import scala.concurrent.Await
import scala.concurrent.duration._

class ProjectMetadataSpec extends Specification with BuildMyApp {
  sequential
  protected val application = new GuiceApplicationBuilder()
    .overrides(bind[DatabaseConfigProvider].to[TestDatabase.testDbProvider])
    .build

  private val logger = Logger(getClass)
  private val injector = application.injector

  protected val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
  protected implicit val db = dbConfigProvider.get[JdbcProfile].db

  "ProjectMetadata.setBulk" should {
    "add a Map of keys to the database" in new WithApplication(buildApp){
      private val injector = app.injector

      protected val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      protected implicit val db = dbConfigProvider.get[PostgresProfile].db

      val result = Await.result(ProjectMetadata.setBulk(4,Map("key"->"value","key_two"->"value_two")),10 seconds)

      result must beSuccessfulTry(2)

      val getResult = Await.result(ProjectMetadata.entryFor(4,"key"), 10 seconds)
      getResult must beSome
      getResult.get.key mustEqual "key"
      getResult.get.projectRef mustEqual 4
      getResult.get.value mustEqual Some("value")
    }

    "not fail when updating keys" in new WithApplication(buildApp){
      private val injector = app.injector

      protected val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      protected implicit val db = dbConfigProvider.get[PostgresProfile].db


      val result = Await.result(ProjectMetadata.setBulk(4,Map("key"->"other value","key_two"->"other value_two")),10 seconds)

      result must beSuccessfulTry(2)
    }
  }

  "ProjectMetadata.entryFor" should {
    "retrieve the value of a key that has been previously set" in new WithApplication(buildApp){
      private val injector = app.injector

      protected val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      protected implicit val db = dbConfigProvider.get[PostgresProfile].db

      val result = Await.result(ProjectMetadata.entryFor(2,"first_key"), 10 seconds)
      result must beSome(ProjectMetadata(Some(1),2,"first_key",Some("first value")))
    }

    "return None if a key is not set" in new WithApplication(buildApp){
      private val injector = app.injector

      protected val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      protected implicit val db = dbConfigProvider.get[PostgresProfile].db

      val result = Await.result(ProjectMetadata.entryFor(2,"dfjkhsfd"), 10 seconds)
      result must beNone

      val secondResult = Await.result(ProjectMetadata.entryFor(99,"first_key"), 10 seconds)
      result must beNone
    }
  }

  "ProjectMetadata.allMetadataFor" should {
    "return all of the metadata keys for the given project" in new WithApplication(buildApp){
      private val injector = app.injector

      protected val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      protected implicit val db = dbConfigProvider.get[PostgresProfile].db

      val result = Await.result(ProjectMetadata.allMetadataFor(2), 10 seconds)
      result must beSuccessfulTry
      val returnedSeq = result.get
      returnedSeq.length mustEqual 2
      returnedSeq.head.projectRef mustEqual 2
      returnedSeq.head.key mustEqual "first_key"
      returnedSeq.head.value must beSome("first value")
      returnedSeq(1).projectRef mustEqual 2
      returnedSeq(1).key mustEqual "second_key"
      returnedSeq(1).value must beSome("second value")
    }
  }
}
