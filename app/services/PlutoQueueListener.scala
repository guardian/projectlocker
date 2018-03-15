package services

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}

import org.redisson.client.codec.StringCodec
import play.api.{Configuration, Logger}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class PlutoQueueListener @Inject() (playConfig:Configuration) extends Redisson {
  val logger = Logger(getClass)
  val config = playConfig

  listen("message-test")
  def listen(queuename:String):Future[Unit] = Future {
    val client = getRedissonClient
    val q = client.getBlockingDeque[String](queuename)
    logger.info("Setting up queue listener")
    while(true){
      val msg = q.pollFirst(30, TimeUnit.SECONDS)
      logger.info(s"Got message: $msg")
    }

  }
}
