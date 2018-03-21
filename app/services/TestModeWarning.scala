package services

import javax.inject.Inject

import akka.actor.ActorSystem
import helpers.DirectoryScanner
import play.api.Logger

import scala.util.{Failure, Success}
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global
/**
  * presents a warning in the log if CI mode is set
  * @param actorSystem
  */
class TestModeWarning @Inject() (actorSystem:ActorSystem){
  private val logger=Logger(getClass)


  val cancellable = actorSystem.scheduler.schedule(1 second,60 seconds) {
    if(sys.env.contains("CI")){
      logger.error("SYSTEM IS IN TEST MODE BECAUSE ENVIRONMENT VARIABLE CI IS SET")
    }
  }
}
