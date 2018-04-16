package services.actors.creation

import java.util.UUID

import akka.actor.Props

import scala.util.{Failure, Success}


object GetStorageDriver {
  def props = Props[GetStorageDriver]

}

/**
  * returns the storage driver associated with the project request.
  * No rollback necessary
  */
class GetStorageDriver extends GenericCreationActor {
  override val persistenceId = "creation-get-storage-actor"

  import GetStorageDriver._
  import GenericCreationActor._

  override def receiveCommand: Receive = {
    case driverRequest:NewProjectRequest=>
      doPersistedSync(driverRequest) { (msg,originalSender)=>
        logger.debug("persisted driver request event to journal, now performing")
        val originalSender = sender()
        driverRequest.rq.destinationStorage.getStorageDriver match {
          case None=>
            logger.warn(s"No storage driver was configured for ${driverRequest.rq.destinationStorage}")
            Failure(new RuntimeException(s"No storage driver was configured for ${driverRequest.rq.destinationStorage}"))
          case Some(storageDriver)=>
            originalSender ! storageDriver
            Success(logger.info(s"Got storage driver: $storageDriver"))
        }
      }
    case rollbackRequest:NewProjectRollback=>
      logger.debug("no rollback needed for this actor")
    case _=>
      super.receiveCommand
  }
}
