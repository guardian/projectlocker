package services.actors

import java.util.UUID

import models.messages.QueuedMessage
import services.actors.MediaAtomUpdateActor.MediaAtomEvent

case class MediaAtomUpdateState(events: Map[UUID, MediaAtomEvent] = Map()){
  def updated(evt: MediaAtomEvent): MediaAtomUpdateState = copy(events ++ Map(evt.eventId->evt))
  def removed(evt: MediaAtomEvent): MediaAtomUpdateState = copy(events.filter(_._1 != evt.eventId))
  def removed(eventId: UUID): MediaAtomUpdateState = copy(events.filter(_._1 != eventId))
  def size:Int = events.size

  def foreach(block: ((UUID, MediaAtomEvent))=>Unit):Unit = events.foreach(block)

  override def toString:String = events.toString()
}
