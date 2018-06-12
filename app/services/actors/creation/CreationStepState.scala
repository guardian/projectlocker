package services.actors.creation

import java.util.UUID

import services.actors.MessageProcessorActor.MessageEvent
import services.actors.MessageProcessorState
import services.actors.creation.GenericCreationActor.CreationEvent

case class CreationStepState(events: Map[UUID, CreationEvent] = Map()) {
  def updated(evt: CreationEvent): CreationStepState = copy(events ++ Map(evt.eventId->evt))
  def removed(evt: CreationEvent): CreationStepState = copy(events.filter(_._1 != evt.eventId))
  def removed(eventId: UUID): CreationStepState = copy(events.filter(_._1 != eventId))
  def size:Int = events.size

  def foreach(block: ((UUID, CreationEvent))=>Unit):Unit = events.foreach(block)

  override def toString:String = events.toString()
}
