package services

import javax.inject.{Inject, Singleton}

import com.redis._
import akka.actor.{ActorSystem, Props}
import play.api.{Configuration, Logger}


@Singleton
class PlutoMessengerProcesser @Inject()(config:Configuration, system:ActorSystem) {
  private val logger = Logger(getClass)

  logger.info("Starting up PlutoMessenger subscriber")

  val r = new RedisClient(config.getOptional[String]("redis.host").getOrElse("localhost"), config.getOptional[Int]("redis.port").getOrElse(6379))
  val s = system.actorOf(Props(new Subscriber(r)))

  s ! Register(callback)

  sub("testchannel")

  def sub(channels: String*) = s ! Subscribe(channels.toArray)

  def unsub(channels: String *) = s ! Unsubscribe(channels.toArray)

  def callback(pubsub: PubSubMessage) = pubsub match {
    case S(channel, no) => logger.debug(s"subscribed to $channel and count = $no")
    case U(channel, no) => logger.debug(s"unsubscribed from $channel and count = $no")
    case E(excep) => logger.error("PlutoMessageProcessor received exception: ", excep)
    case M(channel, msg) => msg match {
      // exit will unsubscribe from all channels and stop subscription service
      case "exit" =>
        println("unsubscribe all ..")
        r.unsubscribe

      // message "+x" will subscribe to channel x
      case x if x startsWith "+" =>
        val s: Seq[Char] = x
        s match {
          case Seq('+', rest @ _*) => r.subscribe(rest.toString){ m => }
        }

      // message "-x" will unsubscribe from channel x
      case x if x startsWith "-" =>
        val s: Seq[Char] = x
        s match {
          case Seq('-', rest @ _*) => r.unsubscribe(rest.toString)
        }

      // other message receive
      case x =>
        logger.warn("received message on channel " + channel + " as : " + x)
    }
  }
}
