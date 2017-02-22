package actors
import actors.ProjectCreationActor.CreateProject
import akka.actor.Actor
import akka.actor.Props
import akka.event.Logging
import models._
import play.api.db.slick.DatabaseConfigProvider
import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import helpers.StorageHelper
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import helpers.StorageActions

import scala.util.{Failure, Success, Try}

/**
  * Created by localhome on 21/02/2017.
  */

object ProjectCreationActor {
  case class CreateProject(newPath: String, projectTypeId: Int, projectTemplateId: Int, primaryStorageId: Option[Int])
}

@Singleton
class ProjectCreationActor @Inject() (configuration: Configuration, dbConfigProvider: DatabaseConfigProvider) extends Actor {
  import ProjectCreationActor._
  implicit val log = Logging(context.system, this)
  implicit val dbConfig:DatabaseConfig[JdbcProfile]=dbConfigProvider.get[JdbcProfile]

  override def receive = {
    case CreateProject(newPath, projectTypeId, projectTemplateId, primaryStorageId)=>
      sender ! createJobRecord(newPath, projectTypeId, projectTemplateId, primaryStorageId)
      doCreateProject(newPath, projectTypeId,projectTemplateId,primaryStorageId)
    case other:String=>
      log.error(s"unknown message received: $other")
  }

  def createJobRecord(newPath:String, projectTypeId: Int,projectTemplateId: Int,primaryStorageId: Option[Int]) = {
    5
  }

  def convertFutureResult(dbFutureResult:Try[Seq[Any]],objectId:Int,objectClass:String) = dbFutureResult match {
    case Success(resultSeq)=>
      if(resultSeq.length!=1){
        throw new RuntimeException(s"Could not find $objectClass with ID $objectId")
      } else {
        Success(resultSeq.head)
      }
    case Failure(error)=>throw new RuntimeException(error)
  }

  def doCreateProject(newPath:String, projectTypeId: Int,projectTemplateId: Int,primaryStorageId: Option[Int]) = {
    log.info("create message received")

    val defaultStorageEntryFuture = StorageHelper.defaultStorage
    val projectTypeFuture = dbConfig.db.run(
      TableQuery[ProjectTypeRow].filter(_.id===projectTypeId).result.asTry
    ).map(convertFutureResult(_,projectTypeId,"ProjectType"))

    val projectTemplateFuture = dbConfig.db.run(
      TableQuery[ProjectTemplateRow].filter(_.id===projectTemplateId).result.asTry
    ).map(convertFutureResult(_,projectTypeId,"ProjectTemplate"))

    Future.sequence(List(projectTypeFuture,projectTemplateFuture,defaultStorageEntryFuture)).onComplete({
      case Success(futureList)=>
        val projectType = futureList.head.asInstanceOf[Try[ProjectType]].get
        val projectTemplate = futureList(1).asInstanceOf[Try[ProjectTemplate]].get
        val destStorageActions = StorageActions.helperFromStorageEntry(
          futureList(2).asInstanceOf[Try[StorageEntry]].get //this should be safe as we should only get this branch if all futures succeeded
        )

        makeProjectGuts(newPath,
          projectType,
          projectTemplate,
          destStorageActions.get
        )
      case Failure(errors)=>
        log.error(errors.toString)
    })
  }

  def makeProjectGuts(newPath:String,
                      projectType:ProjectType, projectTemplate:ProjectTemplate,
                      destStorageActions:StorageActions):Boolean = {

    val sourceStorageFuture = projectTemplate.storage.map(entry=>StorageActions.helperFromStorageEntry(entry))

    sourceStorageFuture.onSuccess({
      case Some(sourceStorageActions)=>
        val readBuffer = sourceStorageActions.readFile(projectTemplate.filePath)
        val writeBuffer = destStorageActions.writeFile(newPath,Map())
    })
    true
  }
}
