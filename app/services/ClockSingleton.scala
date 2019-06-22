package services

import akka.actor.{Actor, ActorRef, ActorSystem, Timers}
import javax.inject.{Inject, Named}
import play.api.Configuration
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
                               )(implicit system:ActorSystem) extends Actor with Timers{
  import ClockSingleton._

  def setupDelays(): Unit = {
    val d = durationToPair(Duration(config.getOptional[String]("pluto.resend_delay").getOrElse("10 seconds")))
    val delay = FiniteDuration(d._1,d._2)

    timers.startPeriodicTimer(ResendTick, ResendTick, delay)

    timers.startPeriodicTimer(RapidClockTick, RapidClockTick, 30.seconds)
    timers.startPeriodicTimer(SlowClockTick, SlowClockTick, 2.minutes)
    timers.startPeriodicTimer(VerySlowClockTick, VerySlowClockTick, 1.hours)
  }

  override def receive: Receive = {
    case RapidClockTick=>
    case SlowClockTick=>
      postrunActionScanner ! PostrunActionScanner.Rescan
    case VerySlowClockTick=>
    case ResendTick=>
      messageProcessorActor ! MessageProcessorActor.RetryFromState()

  }
}
