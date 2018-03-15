package controllers

import javax.inject.{Inject, Singleton}

import play.api.mvc.{AbstractController, ControllerComponents}
import services.PlutoMessengerSender

@Singleton
class MessageTest @Inject() (cc:ControllerComponents, sender:PlutoMessengerSender) extends AbstractController(cc) {
  def test = Action {
    sender.publish("testchannel", "hello world")
    Ok("test publish succeeded")
  }
}
