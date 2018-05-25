package ProjectCreation
import java.io.{File, FileInputStream}
import java.sql.Timestamp
import java.time.LocalDateTime

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import models.{FileEntry, ProjectRequest, ProjectRequestFull}
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


class CopySourceFileSpec extends Specification with BuildMyApp {
  implicit val timeout:akka.util.Timeout = 30.seconds

  "CopySourceFile->CopySourceFileRequest" should {
    "copy a source file to a destination file specified" in new WithApplication(buildApp){
      private val injector = app.injector

      private val dbConfigProvider = injector.instanceOf(classOf[DatabaseConfigProvider])
      private implicit val system = injector.instanceOf(classOf[ActorSystem])
      private implicit val db = dbConfigProvider.get[JdbcProfile].db

      val ac = system.actorOf(Props(new CopySourceFile(dbConfigProvider)))

      val testFileNameSrc = "/tmp/realfile"
      val testFileNameDest = "/tmp/testprojectfile"

      val fileEntrySource = Await.result(FileEntry.entryFor("realfile",1), 2 seconds)
      fileEntrySource must beSuccessfulTry
      fileEntrySource.get.length mustEqual 1

      val fileEntryDest = Await.result(FileEntry.entryFor("testprojectfile",1), 2 seconds)
      fileEntryDest must beSuccessfulTry
      fileEntryDest.get.length mustEqual 1
      fileEntryDest.get.head.hasContent must beFalse

      //need to update THIS LINE to have a correctly valid template path pointing to fileEntrySource
      val maybeRq = Await.result(ProjectRequest("testprojectfile",1,"Test project entry", 1, "test-user", None, None).hydrate, 10 seconds)
      maybeRq must beSome

      /* create a test file */
      Seq("/bin/dd","if=/dev/urandom",s"of=$testFileNameSrc","bs=1k","count=600").!

      val checksumSource = Seq("shasum","-a","1",testFileNameSrc) #| "cut -c 1-40" !!

      val sourceFile = new File(testFileNameSrc)
      val sourceStream = new FileInputStream(sourceFile)

      val destFile = new File(testFileNameDest)
      if(destFile.exists()) destFile.delete()

      val initialData = ProjectCreateTransientData(Some(fileEntryDest.get.head), None)
      val msg = NewProjectRequest(maybeRq.get,None,initialData)
      val result = Await.result((ac ? msg).mapTo[CreationMessage], 10 seconds)
      println(result.toString)
      result must beAnInstanceOf[StepSucceded]
      println(result.asInstanceOf[StepSucceded].updatedData)
      result.asInstanceOf[StepSucceded].updatedData.destFileEntry must beSome
      result.asInstanceOf[StepSucceded].updatedData.destFileEntry.get.hasContent must beTrue
    }
  }


}
