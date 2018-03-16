package controllers

import java.time.ZonedDateTime
import javax.inject.{Inject, Singleton, Named}

import akka.actor.{ActorRef, ActorSystem, Props}
import models._
import models.messages._
import play.api.db.slick.DatabaseConfigProvider
import play.api.{Configuration, Logger}
import play.api.mvc.{AbstractController, ControllerComponents}
import services.actors.MessageProcessorActor
import slick.jdbc.PostgresProfile

import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class MessageTest @Inject() (@Named("message-processor-actor") messageProcessor:ActorRef, cc:ControllerComponents, playConfig:Configuration,
                             dbConfigProvider: DatabaseConfigProvider, actorSystem:ActorSystem)
  extends AbstractController(cc) {
  val logger = Logger(getClass)
  val config = playConfig

  implicit val db = dbConfigProvider.get[PostgresProfile].db

  def test = Action {
    messageProcessor ! "Hello world"
    Ok("test publish succeeded")
  }


  def testassetfolder = Action {
    val msg = NewAssetFolder("/path/to/newassetfolder",Some(1),Some("VX-1234"))

    messageProcessor! msg
    Ok("test publish succeeded")
  }

  def sendCreateMessageToSelf(createdProjectEntry: ProjectEntry, projectTemplate: ProjectTemplate):Future[Unit] = {
    Future.sequence(Seq(
      projectTemplate.projectType,
      createdProjectEntry.getCommission
    )).map(results=>{
      val projectType = results.head.asInstanceOf[ProjectType]
      val maybeCommission = results(1).asInstanceOf[Option[PlutoCommission]]

      if(maybeCommission.isDefined){
        messageProcessor ! NewProjectCreated(createdProjectEntry,
          projectType,
          maybeCommission.get,
          ZonedDateTime.now().toEpochSecond
        )
      } else {
        logger.error(s"Can't sync project ${createdProjectEntry.projectTitle} (${createdProjectEntry.id}) to Pluto - missing commission")
      }
    })
  }

  def testprojectcreate = Action.async {
    Future.sequence(Seq(ProjectEntry.entryForId(6), ProjectTemplate.entryFor(1))).flatMap(results=>{
      results.head.asInstanceOf[Try[ProjectEntry]] match {
        case Success(entry) =>
          sendCreateMessageToSelf(entry, results(1).asInstanceOf[Option[ProjectTemplate]].get).map(unit=>{
            Ok(s"test queue succeeded")
          })
        case Failure(error) =>
          Future(InternalServerError(error.toString))
      }
    })
  }
}
