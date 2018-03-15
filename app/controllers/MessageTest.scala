package controllers

import javax.inject.{Inject, Singleton}

import models.messages.{NewAssetFolder, NewAssetFolderSerializer}
import org.redisson.api.RedissonClient
import org.redisson.client.codec.StringCodec
import play.api.{Configuration, Logger}
import play.api.mvc.{AbstractController, ControllerComponents}
import services.{PlutoMessengerSender, Redisson}

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class MessageTest @Inject() (cc:ControllerComponents, sender:PlutoMessengerSender, playConfig:Configuration) extends AbstractController(cc) with Redisson with NewAssetFolderSerializer {
  val logger = Logger(getClass)
  val config = playConfig

  implicit val redissonClient:RedissonClient = getRedissonClient

  def test = Action {
    sender.publish("testchannel", "hello world")
    Ok("test publish succeeded")
  }

  def testqueue = Action.async {
    queueMessage("message-test","hello world",None).map({
      case Success(value)=>Ok(s"test queue succeeded: $value")
      case Failure(error)=>
        logger.error("could not send test message: ", error)
        InternalServerError(error.toString)
    })
  }

  def testassetfolder = Action.async {
    val msg = NewAssetFolder("/path/to/newassetfolder",Some(1),Some("VX-1234"))
    queueMessage("projectlocker-pluto-assetfolder", msg,None).map({
      case Success(value)=>Ok(s"test queue succeeded: $value")
      case Failure(error)=>
        logger.error("could not send test message: ", error)
        InternalServerError(error.toString)
    })
  }
}
