package services.actors

import services.actors.creation.GenericCreationActor

class ProjectCreationActor extends GenericCreationActor{
  override val persistenceId = "project-creation-actor"

  import GenericCreationActor._

  def runNextActorInSequence(actorSequence:Seq[GenericCreationActor]):Either[]
  override def receiveCommand: Receive = {
    case rq:NewProjectRequest=>

  }
}
