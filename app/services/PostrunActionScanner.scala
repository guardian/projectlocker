package services

import java.io.File
import java.sql.Timestamp

import akka.actor.ActorSystem
import com.google.inject.{Inject, Singleton}
import play.api.{Configuration, Logger}
import helpers.{DirectoryScanner, JythonRunner, PrecompileException}
import models.PostrunAction
import java.time.{Instant, ZonedDateTime}

import org.reflections.Reflections
import org.reflections.scanners.{ResourcesScanner, SubTypesScanner}
import org.reflections.util.{ClasspathHelper, ConfigurationBuilder, FilterBuilder}
import org.slf4j.MDC
import play.api.db.slick.DatabaseConfigProvider
import postrun.PojoPostrun
import slick.jdbc.PostgresProfile

import collection.JavaConverters._
import collection.mutable._
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

  protected def addIfNotExists(scriptName:String,absolutePath:String) = {
    PostrunAction.entryForRunnable(scriptName) map {
      case Success(results)=>
        if(results.isEmpty){
          logger.info(s"Adding newly found postrun script $absolutePath to database")
          val newRecord = PostrunAction(None,scriptName,scriptName,None,"system",1,new Timestamp(ZonedDateTime.now().toEpochSecond*1000))
          newRecord.save map {
            case Failure(error)=>
              logger.error("Unable to save postrun script to database: ", error)
            case Success(newPostrunAction)=>
              logger.info(s"Saved postrun action for $scriptName with id of ${newPostrunAction.id.get}")
          }
        } else {
          logger.debug(s"Script $absolutePath is already present in database")
        }
      case Failure(error)=>
        logger.error("Could not look up script:", error)
    }
  }

  protected def addFileIfNotExists(scriptFile: File) = {
    addIfNotExists(scriptFile.getName, scriptFile.getAbsolutePath)
  }

  val cancellable = actorSystem.scheduler.schedule(1 second,60 seconds) {
    logger.debug("Rescanning postrun actions")

    val scriptsDir = config.get[String]("postrun.scriptsPath")
    MDC.put("scripts_dir", scriptsDir)
    DirectoryScanner.scanAll(scriptsDir).map({
      case Failure(error)=>
        logger.error(s"Could not scan $scriptsDir: ", error)
      case Success(filesList)=>
        filesList
            .filter(file=>file.getName.endsWith(".py") && ! file.getName.startsWith("__"))
            .foreach(file=>addFileIfNotExists(file))
    })

    //Scan POJOs
    val classLoadersList = ArrayBuffer(ClasspathHelper.contextClassLoader, ClasspathHelper.staticClassLoader)
    val reflections = new Reflections(new ConfigurationBuilder()
    .setScanners(new SubTypesScanner(false), new ResourcesScanner())
    .setUrls(ClasspathHelper.forClassLoader(classLoadersList.head, classLoadersList(1)))
        .filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix("postrun")))
    )

    reflections.getSubTypesOf(classOf[PojoPostrun]).asScala.foreach(classRef=>addIfNotExists(classRef.getCanonicalName,classRef.getCanonicalName))
  }
}
