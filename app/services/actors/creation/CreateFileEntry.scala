package services.actors.creation

import java.sql.Timestamp
import java.time.LocalDateTime

import akka.actor.Props
import drivers.StorageDriver
import akka.pattern.ask
import exceptions.ProjectCreationError
import models.{FileEntry, ProjectRequestFull, ProjectType}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object CreateFileEntry {

}

class CreateFileEntry extends GenericCreationActor {
  override val persistenceId = "create-file-entry"

  import GenericCreationActor._

  /**
    * Combines the provided filename with a (possibly) provided extension
    * @param filename filename
    * @param extension Option possibly containing a string of the file extension
    * @return Combined filename and extension. If no extension, filename returned unchanged; if the extension does not start with a
    *         dot then a dot is inserted between name and extension
    */
  private def makeFileName(filename:String,extension:Option[String]):String = {
    if(extension.isDefined){
      if(extension.get.startsWith("."))
        s"$filename${extension.get}"
      else
        s"$filename.${extension.get}"
    } else
      filename
  }

  /**
    * Either create a new file entry for the required destination file or retrieve a pre-exisiting one
    * @param rq ProjectRequestFull instance describing the project to be created
    * @param recordTimestamp time to record for creation
    * @param db implicitly provided database instance
    * @return a Future, containing a Try, containing a saved FileEntry instance if successful
    */
  def getDestFileFor(rq:ProjectRequestFull, recordTimestamp:Timestamp)(implicit db: slick.jdbc.PostgresProfile#Backend#Database): Future[Try[FileEntry]] =
    FileEntry.entryFor(rq.filename, rq.destinationStorage.id.get).flatMap({
      case Success(filesList)=>
        if(filesList.isEmpty) {
          //no file entries exist already, create one and proceed
          ProjectType.entryFor(rq.projectTemplate.projectTypeId) map {
            case Success(projectType)=>
              Success(FileEntry(None, makeFileName(rq.filename,projectType.fileExtension), rq.destinationStorage.id.get, "system", 1,
                recordTimestamp, recordTimestamp, recordTimestamp, hasContent = false, hasLink = false))
            case Failure(error)=>Failure(error)
          }
        } else {
          //a file entry does already exist, but may not have data on it
          if(filesList.length>1)
            Future(Failure(new ProjectCreationError(s"Multiple files exist for ${rq.filename} on ${rq.destinationStorage.repr}")))
          else if(filesList.head.hasContent)
            Future(Failure(new ProjectCreationError(s"File ${rq.filename} on ${rq.destinationStorage.repr} already has data")))
          else
            Future(Success(filesList.head))
        }
      case Failure(error)=>Future(Failure(error))
    })

  override def receiveCommand: Receive = {
    case entryRequest:NewProjectRequest=>
//      val sdActor = context.actorOf(Props(GetStorageDriver.getClass))
//      val storageDriverFuture = (sdActor ? entryRequest).map(_.asInstanceOf[StorageDriver])
//      storageDriverFuture.map(storageDriver=>{
//
//      })
      val originalSender = sender()
      val recordTimestamp = Timestamp.valueOf(entryRequest.createTime.getOrElse(LocalDateTime.now()))
      getDestFileFor(entryRequest.rq, recordTimestamp).map({
        case Success(fileEntry)=>
          val copyActor = context.actorOf(Props(CopyFileActor.getClass))
          originalSender ! (copyActor ? entryRequest)
        case Failure(error)=>
          logger.error("Could not create destination file record", error)
          originalSender ! StepFailed(error)
      })
    case _=>
      super.receiveCommand
  }
}
