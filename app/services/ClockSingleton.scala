package services

import akka.actor.{Actor, ActorRef, ActorSystem, Timers}
import javax.inject.{Inject, Named}
import play.api.{Configuration, Logger}
import services.actors.MessageProcessorActor

import scala.concurrent.duration._

object ClockSingleton {
  trait CSMsg

  case object RapidClockTick
  case object SlowClockTick
  case object VerySlowClockTick
  case object ResendTick
}

class ClockSingleton @Inject() (config:Configuration,
                               @Named("message-processor-actor") messageProcessorActor:ActorRef,
                               @Named("postrun-action-scanner") postrunActionScanner:ActorRef,
                               @Named("pluto-wg-commission-scanner") plutoWGCommissionScanner:ActorRef,
                               )(implicit system:ActorSystem) extends Actor with Timers{
  import ClockSingleton._
  private val logger = Logger(getClass)

  initialise()

  def initialise(): Unit = {
    logger.info("In ClockSingleton initialise, setting up timers")
    val d = durationToPair(Duration(config.getOptional[String]("pluto.resend_delay").getOrElse("10 seconds")))
    val delay = FiniteDuration(d._1,d._2)

    timers.startPeriodicTimer(ResendTick, ResendTick, delay)

    timers.startPeriodicTimer(RapidClockTick, RapidClockTick, 30.seconds)
    timers.startPeriodicTimer(SlowClockTick, SlowClockTick, 5.minutes)
    timers.startPeriodicTimer(VerySlowClockTick, VerySlowClockTick, 1.hours)
    logger.info(s"Timer setup complete")
  }

  override def receive: Receive = {
    case RapidClockTick=>
      logger.debug("RapidClockTick")
    case SlowClockTick=>
      logger.debug("SlowClockTick")
      plutoWGCommissionScanner ! PlutoWGCommissionScanner.RefreshWorkingGroups

    case VerySlowClockTick=>
      logger.debug("VerySlowClockTick")
      postrunActionScanner ! PostrunActionScanner.Rescan

    case ResendTick=>
      logger.debug("ResendTick")
      messageProcessorActor ! MessageProcessorActor.RetryFromState()

  }
}
