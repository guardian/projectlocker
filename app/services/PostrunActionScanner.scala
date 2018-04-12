package services

import java.io.File
import java.sql.Timestamp

import akka.actor.ActorSystem
import com.google.inject.{Inject, Singleton}
import play.api.{Configuration, Logger}
import helpers.{DirectoryScanner, JythonRunner, PrecompileException}
import models.PostrunAction
import java.time.{Instant, ZonedDateTime}

import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.PostgresProfile

import scala.concurrent.duration._
import scala.util.{Failure, Success}

@Singleton
class PostrunActionScanner @Inject() (dbConfigProvider: DatabaseConfigProvider, config:Configuration, actorSystem: ActorSystem) {
  private val logger = Logger(this.getClass)
  import actorSystem.dispatcher

  implicit val db = dbConfigProvider.get[PostgresProfile].db
  implicit val configImplicit = config

  //call out to JythonRunner to ensure that scripts are precompiled when we start up.
  JythonRunner.precompile.map(results=>{
    results.foreach({
      case Success(runnable)=>
        logger.debug(s"Successfully precompiled $runnable")
      case Failure(error)=>error match {
        case e:PrecompileException=>
          logger.error(s"Could not precompile ${e.toString}", error)
        case _=>
          logger.error("Could not precompile: ", error)
      }
    })
  }).recover({
    case e:Throwable=>
      logger.error("Precompiler could not recover, this should not happen", e)
  })

  protected def addIfNotExists(scriptFile: File) = {
    PostrunAction.entryForRunnable(scriptFile.getName) map {
      case Success(results)=>
        if(results.isEmpty){
          logger.info(s"Adding newly found postrun script ${scriptFile.getAbsolutePath} to database")
          val newRecord = PostrunAction(None,scriptFile.getName,scriptFile.getName,None,"system",1,new Timestamp(ZonedDateTime.now().toEpochSecond*1000))
          newRecord.save map {
            case Failure(error)=>
              logger.error("Unable to save postrun script to database: ", error)
            case Success(newPostrunAction)=>
              logger.info(s"Saved postrun action for ${scriptFile.getName} with id of ${newPostrunAction.id.get}")
          }
        } else {
          logger.debug(s"Script ${scriptFile.getAbsolutePath} is already present in database")
        }
      case Failure(error)=>
        logger.error("Could not look up script:", error)
    }
  }

  val cancellable = actorSystem.scheduler.schedule(1 second,60 seconds) {
    logger.debug("Rescanning postrun actions")

    val scriptsDir = config.get[String]("postrun.scriptsPath")
    DirectoryScanner.scanAll(scriptsDir).map({
      case Failure(error)=>
        logger.error(s"Could not scan $scriptsDir: ", error)
      case Success(filesList)=>
        filesList
            .filter(file=>file.getName.endsWith(".py") && ! file.getName.startsWith("__"))
            .foreach(file=>addIfNotExists(file))
    })
  }
}
