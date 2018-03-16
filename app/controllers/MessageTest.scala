package controllers

import java.time.ZonedDateTime
import javax.inject.{Inject, Singleton}

import models._
import models.messages._
import org.redisson.api.RedissonClient
import org.redisson.client.codec.StringCodec
import play.api.db.slick.DatabaseConfigProvider
import play.api.{Configuration, Logger}
import play.api.mvc.{AbstractController, ControllerComponents}
import services.{PlutoMessengerSender, Redisson}
import slick.jdbc.PostgresProfile

import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class MessageTest @Inject() (cc:ControllerComponents, sender:PlutoMessengerSender,
                             playConfig:Configuration, dbConfigProvider: DatabaseConfigProvider)
  extends AbstractController(cc) with Redisson with NewAssetFolderSerializer with NewProjectCreatedSerializer {
  val logger = Logger(getClass)
  val config = playConfig

  implicit val db = dbConfigProvider.get[PostgresProfile].db

  implicit val redissonClient:RedissonClient = getRedissonClient

  def test = Action {
    sender.publish("testchannel", "hello world")
    Ok("test publish succeeded")
  }

  def testqueue = Action.async {
    queueMessage("message-test","hello world",None).map({
      case Success(value)=>Ok(s"test queue succeeded: $value")
      case Failure(error)=>
        logger.error("could not send test message: ", error)
        InternalServerError(error.toString)
    })
  }

  def testassetfolder = Action.async {
    val msg = NewAssetFolder("/path/to/newassetfolder",Some(1),Some("VX-1234"))
    queueMessage(NamedQueues.ASSET_FOLDER, msg,None).map({
      case Success(value)=>Ok(s"test queue succeeded: $value")
      case Failure(error)=>
        logger.error("could not send test message: ", error)
        InternalServerError(error.toString)
    })
  }

  def sendCreateMessageToSelf(createdProjectEntry: ProjectEntry, projectTemplate: ProjectTemplate):Future[Unit] = {
    Future.sequence(Seq(
      projectTemplate.projectType,
      createdProjectEntry.getCommission
    )).map(results=>{
      val projectType = results.head.asInstanceOf[ProjectType]
      val maybeCommission = results(1).asInstanceOf[Option[PlutoCommission]]

      logger.info(s"${maybeCommission}")
      logger.info(s"$projectType")

      if(maybeCommission.isDefined){
        queueMessage(NamedQueues.PROJECT_CREATE,
          NewProjectCreated(createdProjectEntry,
            projectType,
            maybeCommission.get,
            ZonedDateTime.now().toEpochSecond), None)
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
