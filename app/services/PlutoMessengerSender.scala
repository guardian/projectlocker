package services

import com.google.inject.ImplementedBy

@ImplementedBy(classOf[PlutoMessengerSenderImpl])
trait PlutoMessengerSender {
  def publish(channel:String, message:String)
}
