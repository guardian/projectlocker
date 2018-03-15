package services

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import org.redisson._
import org.redisson.api.RedissonClient
import org.redisson.config.{Config, TransportMode}
import play.api.libs.json.{Json, Writes}
import play.api.{Configuration, Logger}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

trait Redisson {
  val config:Configuration  //implemented by user
  protected val logger:Logger

  def getRedissonClient = {
    val redissonConfig = new Config()
    val serverAddress = s"redis://${config.getOptional[String]("redis.host").getOrElse("localhost")}:${config.getOptional[Int]("redis.port").getOrElse(6379)}"
    logger.info(s"Connecting to redis at $serverAddress")

    redissonConfig.setTransportMode(TransportMode.NIO)
    redissonConfig.setThreads(3)
    redissonConfig.setNettyThreads(3)
    redissonConfig.useSingleServer().setAddress(serverAddress)
    Redisson.create(redissonConfig)
  }

  def retry[T](n: Int)(fn: =>T): Try[T] = {
    try {
      Success(fn)
    } catch {
      case ex: Throwable =>
        logger.error("Could not send redis message. Retrying after 1s...", ex)
        if(n>1)
          retry(n -1)(fn)
        else
          Failure(ex)
    }
  }

  //def makeJson[T](message:T)(implicit writes:Writes[T]) = Json.toJson(message).toString()

  def queueMessage[T](queuename:String, message:T, maybeDelay: Option[Duration])
                     (implicit client:RedissonClient, writes:Writes[T]) = Future {
    retry(5) {
      val q = client.getBlockingQueue[String](queuename)
      val messageAsString = Json.toJson(message).toString
      maybeDelay match {
        case None=>q.add(messageAsString)
        case Some(delay)=>
          val delayedQueue = client.getDelayedQueue(q)
          delayedQueue.offer(messageAsString, delay.toSeconds, TimeUnit.SECONDS)
      }
    }
  }
}
