package services

import akka.actor.ActorSystem
import org.redisson._
import org.redisson.api.RedissonClient
import org.redisson.config.{Config, TransportMode}
import play.api.{Configuration, Logger}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

trait Redisson {
  val config:Configuration  //implemented by user
  val logger:Logger

  def getRedissonClient = {
    val redissonConfig = new Config()
    val serverAddress = s"redis://${config.getOptional[String]("redis.host").getOrElse("localhost")}:${config.getOptional[Int]("redis.port").getOrElse(6379)}"
    logger.info(s"Connecting to redis at $serverAddress")

    redissonConfig.setTransportMode(TransportMode.NIO)
    redissonConfig.useSingleServer().setAddress(serverAddress)
    Redisson.create(redissonConfig)
  }

  def retry[T](n: Int)(fn: =>T): Try[T] = {
    try {
      Success(fn)
    } catch {
      case ex: Throwable =>
        logger.error("Could not send redis message. Retrying after 1s...", ex)
        //Thread.sleep(1000)
        if(n>1)
          retry(n -1)(fn)
        else
          Failure(ex)
    }
  }

  def queueMessage[T](queuename:String, message:T)(implicit client:RedissonClient) = Future {
    retry(5) {
      val q = client.getBlockingQueue[T](queuename)
      q.add(message)
    }
  }
}
