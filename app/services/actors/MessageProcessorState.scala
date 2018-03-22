package services.actors

import java.util.UUID

import models.messages.QueuedMessage
import services.actors.MessageProcessorActor.MessageEvent

case class MessageProcessorState(events: Map[UUID, MessageEvent] = Map()){
  def updated(evt: MessageEvent): MessageProcessorState = copy(events ++ Map(evt.eventId->evt))
  def removed(evt: MessageEvent): MessageProcessorState = copy(events.filter(_._1 != evt.eventId))
  def removed(eventId: UUID): MessageProcessorState = copy(events.filter(_._1 != eventId))
  def size:Int = events.size

  def foreach(block: ((UUID, MessageEvent))=>Unit):Unit = events.foreach(block)

  override def toString:String = events.toString()
}
