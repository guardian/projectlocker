package services

import javax.inject.{Inject, Singleton}

import play.api.{Configuration, Logger}
import com.redis._
import akka.actor.{Actor, ActorSystem, Props}

@Singleton
class PlutoMessengerSenderImpl @Inject()(config:Configuration, system:ActorSystem) extends PlutoMessengerSender {
  private val logger = Logger(getClass)

  logger.info("Starting up PlutoMessenger subscriber")

  val r = new RedisClient(config.getOptional[String]("redis.host").getOrElse("localhost"), config.getOptional[Int]("redis.port").getOrElse(6379))
  val p = system.actorOf(Props(new Publisher(r)))

  def publish(channel: String, message: String) = p ! Publish(channel, message)
}
