package ProjectCreation
import java.io.{File, FileInputStream}
import java.sql.Timestamp
import java.time.LocalDateTime

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import models.{FileEntry, ProjectRequest, ProjectRequestFull}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.db.slick.DatabaseConfigProvider
import play.api.test.WithApplication
import services.actors.creation.{CopySourceFile, CreateFileEntry, CreationMessage}
import services.actors.creation.GenericCreationActor._
import slick.jdbc.{JdbcBackend, JdbcProfile}
import utils.BuildMyApp

import scala.concurrent.{Await, Future}
import scala.sys.process._
import scala.util.{Failure, Try}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import org.mockito.Mockito._
import helpers.StorageHelper

class CopySourceFileSpec extends Specification with BuildMyApp with Mockito {
  implicit val timeout:akka.util.Timeout = 30.seconds

  "CopySourceFile->NewProjectRequest" should {
    "copy a source file to a destination file specified" in new WithApplication(buildApp){
      private val injector = app.injector

      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val system = injector.instanceOf(classOf[ActorSystem])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val fileEntrySource = Await.result(FileEntry.entryFor("realfile",1,1), 2 seconds)
      fileEntrySource must beSuccessfulTry
      fileEntrySource.get.length mustEqual 1

      val fileEntryDest = Await.result(FileEntry.entryFor("testprojectfile",1,1), 2 seconds)
      fileEntryDest must beSuccessfulTry
      fileEntryDest.get.length mustEqual 1
      fileEntryDest.get.head.hasContent must beFalse
      protected val storageHelper = mock[StorageHelper]
      storageHelper.copyFile(any[FileEntry],any[FileEntry])(any) returns Future(Right(fileEntryDest.get.head.copy(hasContent = true)))

      val ac = system.actorOf(Props(new CopySourceFile(dbConfigProvider, storageHelper)))

      val maybeRq = Await.result(ProjectRequest("testprojectfile",1,"Test project entry", 1, "test-user", None, None, false, false, false).hydrate, 10 seconds)
      maybeRq must beSome

      val initialData = ProjectCreateTransientData(Some(fileEntryDest.get.head), None,None)
      val msg = NewProjectRequest(maybeRq.get,None,initialData)
      val result = Await.result((ac ? msg).mapTo[CreationMessage], 10 seconds)
      result must beAnInstanceOf[StepSucceded]
      result.asInstanceOf[StepSucceded].updatedData.destFileEntry must beSome
      result.asInstanceOf[StepSucceded].updatedData.destFileEntry.get.hasContent must beTrue
    }

    "return StepFailed if copy operation indicates an error" in new WithApplication(buildApp){
      private val injector = app.injector

      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val system = injector.instanceOf(classOf[ActorSystem])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val fileEntrySource = Await.result(FileEntry.entryFor("realfile",1,1), 2 seconds)
      fileEntrySource must beSuccessfulTry
      fileEntrySource.get.length mustEqual 1

      val fileEntryDest = Await.result(FileEntry.entryFor("testprojectfile",1,1), 2 seconds)
      fileEntryDest must beSuccessfulTry
      fileEntryDest.get.length mustEqual 1
      fileEntryDest.get.head.hasContent must beFalse
      protected val storageHelper = mock[StorageHelper]
      storageHelper.copyFile(any[FileEntry],any[FileEntry])(any) returns Future(Right(fileEntryDest.get.head.copy(hasContent = true)))

      val ac = system.actorOf(Props(new CopySourceFile(dbConfigProvider, storageHelper)))

      val maybeRq = Await.result(ProjectRequest("testprojectfile",1,"Test project entry", 1, "test-user", None, None, false, false, false).hydrate, 10 seconds)
      maybeRq must beSome

      val initialData = ProjectCreateTransientData(Some(fileEntryDest.get.head), None, None)
      val msg = NewProjectRequest(maybeRq.get,None,initialData)
      val result = Await.result((ac ? msg).mapTo[CreationMessage], 10 seconds)
      result must beAnInstanceOf[StepFailed]
      result.asInstanceOf[StepFailed].updatedData.destFileEntry must beSome
      result.asInstanceOf[StepFailed].updatedData.destFileEntry.get.hasContent must beFalse
      result.asInstanceOf[StepFailed].err.getMessage mustEqual "Something went KABOOM!"
    }
  }

  "CopySourceFile->NewProjectRollback" should {
    "call out to StorageHelper to delete the provided FileEntry from disk" in new WithApplication(buildApp){
      private val injector = app.injector

      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val system = injector.instanceOf(classOf[ActorSystem])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val fileEntrySource = Await.result(FileEntry.entryFor("realfile",1,1), 2 seconds)
      fileEntrySource must beSuccessfulTry
      fileEntrySource.get.length mustEqual 1

      val fileEntryDest = Await.result(FileEntry.entryFor("testprojectfile",1,1 ), 2 seconds)
      fileEntryDest must beSuccessfulTry
      fileEntryDest.get.length mustEqual 1
      fileEntryDest.get.head.hasContent must beFalse

      val mockedStorageHelper = mock[StorageHelper]
      mockedStorageHelper.deleteFile(any[FileEntry])(any) returns Future(Right(fileEntryDest.get.head.copy(hasContent = false)))

      val ac = system.actorOf(Props(new CopySourceFile(dbConfigProvider, mockedStorageHelper)))

      val maybeRq = Await.result(ProjectRequest("testprojectfile",1,"Test project entry", 1, "test-user", None, None, false, false, false).hydrate, 10 seconds)
      maybeRq must beSome

      val initialData = ProjectCreateTransientData(Some(fileEntryDest.get.head), None, None)
      val msg = NewProjectRollback(maybeRq.get,initialData)
      val result = Await.result((ac ? msg).mapTo[CreationMessage], 10 seconds)
      result must beAnInstanceOf[StepSucceded]
      result.asInstanceOf[StepSucceded].updatedData.destFileEntry must beSome
      result.asInstanceOf[StepSucceded].updatedData.destFileEntry.get.hasContent must beFalse
      there was one(mockedStorageHelper).deleteFile(fileEntryDest.get.head)
    }
  }
}
