package services.actors

import models.messages.QueuedMessage

case class MessageProcessorState(events: List[QueuedMessage] = Nil){
  def updated(evt: QueuedMessage): MessageProcessorState = copy(evt :: events)
  def removed(evt: QueuedMessage): MessageProcessorState = copy(events.filter(_ != evt))
  def size:Int = events.length
  override def toString:String = events.reverse.toString()
}
