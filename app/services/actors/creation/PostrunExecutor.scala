package services.actors.creation

import javax.inject.{Inject, Named}

import akka.actor.ActorRef
import helpers.{JythonOutput, PostrunDataCache}
import models._
import models.messages.{NewAdobeUuid, NewAssetFolder}
import org.slf4j.MDC
import play.api.Configuration
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

class PostrunExecutor @Inject() (@Named("message-processor-actor") messageProcessor:ActorRef,
                                 dbConfigProvider:DatabaseConfigProvider,
                                 config:Configuration) extends GenericCreationActor {
  override val persistenceId = "postrun-executor-actor"
  implicit val timeout:Duration = Duration(config.getOptional[String]("postrun.timeout").getOrElse("30 seconds"))

  import GenericCreationActor._
  private implicit val db=dbConfigProvider.get[JdbcProfile].db

  protected def syncExecScript(action: PostrunAction, projectFileName: String, entry: ProjectEntry,
                               projectType: ProjectType, cache: PostrunDataCache,
                               workingGroupMaybe: Option[PlutoWorkingGroup], commissionMaybe: Option[PlutoCommission])
                              (implicit db: slick.jdbc.PostgresProfile#Backend#Database, config:Configuration, timeout: Duration):Try[JythonOutput] =
    Await.result(action.run(projectFileName,entry,projectType,cache, workingGroupMaybe, commissionMaybe), timeout)

  /**
    * Recursively iterates a list of postrun actions, running each
    * @param actions list of actions to run
    * @param results accumulator for results. Initially call this with an empty Seq()
    * @param cache PostrunDataCache instance for passing data between postruns. Initially call this with PostrunDataCache().
    * @param projectFileName file name of the created project
    * @param projectEntry entry of the created project
    * @param projectType type of the created project
    * @param db implicitly passed database object
    * @param config implicitly passed Play framework configuration
    * @return ultimate sequence of results
    */
  def runNextAction(actions: Seq[PostrunAction], results:Seq[Try[JythonOutput]], cache: PostrunDataCache,
                    projectFileName: String, projectEntry: ProjectEntry, projectType: ProjectType,
                    workingGroupMaybe:Option[PlutoWorkingGroup],commissionMaybe:Option[PlutoCommission])
                   (implicit db: slick.jdbc.PostgresProfile#Backend#Database, config:play.api.Configuration, timeout: Duration):Seq[Try[JythonOutput]] = {
    logger.debug(s"runNextAction: remaining actions: ${actions.toString()}")
    actions.headOption match {
      case Some(nextAction)=> //run next action
        logger.debug(s"running action ${nextAction.toString}")
        val scriptResult = syncExecScript(nextAction, projectFileName,projectEntry, projectType, cache, workingGroupMaybe, commissionMaybe)
        val newResults = results ++ Seq(scriptResult)
        logger.debug(s"got results: ${newResults.toString()}")
        scriptResult match {
          case Success(output)=>
            output.raisedError match {
              case None=> // no error raised
                runNextAction(actions.tail, newResults, output.newDataCache, projectFileName, projectEntry, projectType, workingGroupMaybe, commissionMaybe)
              case Some(scriptError)=>  //script ran but failed
                logger.error(s"Postrun script ${nextAction.runnable} failed: ", scriptError)
                logger.error("Aborting postruns due to failure")
                newResults
            }
          case Failure(error)=>
            logger.error(s"Could not start postrun script ${nextAction.runnable}.")
            logger.error("Aborting postruns due to failure")
            newResults
        }

      case None=> //no more actions left to run
        logger.debug("recursion ends")
        results
    }
  }

  /**
    * recursively searches the result list for the first value of the given key in the datastore
    * @param reversedResults a Sequence of JythonOutput. Normally reverse this, to get the final value of a key rather than the first.
    * @return an Option which contains the value, if there is one.
    */
  def locateCacheValue(reversedResults:Seq[JythonOutput],key:String):Option[String] = {
    if(reversedResults.isEmpty) return None

    val scalaMap = reversedResults.head.newDataCache.asScala
    if(scalaMap.contains(key)){
      Some(scalaMap(key))
    } else {
      locateCacheValue(reversedResults.tail,key)
    }
  }

  /**
    * Traverses a sequence of a Try of type A and returns either a Right with all results if they all succeeded or a Left
    * with all of the errors if any failed.
    * https://stackoverflow.com/questions/15495678/flatten-scala-try
    * @param xs - sequence to traverse
    * @tparam A - type of sequence xs
    * @return either Left containing a sequence of Throwable or Right containing sequence of A
    */
  protected def collectFailures[A](xs:Seq[Try[A]]):Either[Seq[Throwable],Seq[A]] =
    Try(Right(xs.map(_.get))).getOrElse(Left(xs.collect({case Failure(err)=>err})))

  protected def persistMetadataToDatabase(maybeOutput: Option[JythonOutput], createdProjectEntry:ProjectEntry, successfulActions:Int, writtenPath: String): Future[Either[String, String]] = maybeOutput match {
      case Some(finalResult)=>
        if(createdProjectEntry.id.isEmpty) throw new RuntimeException("Created project without id?")
        val mdSetFuture = ProjectMetadata.setBulk(createdProjectEntry.id.get,finalResult.newDataCache.asScala)
        mdSetFuture.map({
          case Success(count)=>
            logger.info(s"Set $count metadata fields for project ID ${createdProjectEntry.id}")
            Right(s"Successfully ran $successfulActions postrun actions for project $writtenPath")
          case Failure(err)=>
            logger.error("Could not set metadata for project", err)
            Left(err.toString)
        })
      case None=>
        logger.warn(s"No postruns ran for ${createdProjectEntry.projectTitle} (${createdProjectEntry.id.get} so no metadata")
        Future(Right(s"Successfully ran $successfulActions postrun actions for project $writtenPath"))
  }

  def sendAssetFolderMessage(reversedResults: Seq[JythonOutput], createdProjectEntry:ProjectEntry):Unit =
    locateCacheValue(reversedResults, "created_asset_folder") match {
      case Some(createdAssetFolder) =>
        //It doesn't matter that the vidispine ID is almost certainly not set at this point.  The receiver for the message
        //will use the Projectlocker project id to look it up from the database; if it's still not set the message will be re-sent
        //to the back of the queue with a 1s delay.
        messageProcessor ! NewAssetFolder(createdAssetFolder,
          createdProjectEntry.id,
          createdProjectEntry.vidispineProjectId
        )
      case None =>
        logger.error("No asset folder was set, so I can't inform pluto about a new asset folder.")
    }

  def sendUpdatedUuidMessage(reversedResults: Seq[JythonOutput], createdProjectEntry: ProjectEntry):Unit =
    locateCacheValue(reversedResults, "new_adobe_uuid") match {
      case Some(newUuid)=>
        logger.info(s"Updated adobe uuid to $newUuid, saving update to database and informing pluto")
        messageProcessor ! NewAdobeUuid(createdProjectEntry,newUuid)
      case None=>
        logger.debug("No adobe uuid set, probably not an adobe project")
    }

  override def receiveCommand: Receive = {
    case createRequest:NewProjectRequest=>
      //FIXME: should validate createRequest here, before entering persistence block
      doPersistedAsync(createRequest) { (msg, originalSender) =>
        implicit val configImplicit = config

        val fileEntry = createRequest.data.destFileEntry.get
        MDC.put("fileEntry", fileEntry.toString)
        val createdProjectEntry = createRequest.data.createdProjectEntry.get
        MDC.put("createdProjectEntry", createdProjectEntry.toString)

        try {
          val futureSequence = Future.sequence(Seq(
            createRequest.rq.projectTemplate.projectType,
            fileEntry.getFullPath,
            createdProjectEntry.getWorkingGroup,
            createdProjectEntry.getCommission
          ))

          futureSequence.map({ resultSeq =>
            val projectType = resultSeq.head.asInstanceOf[ProjectType]
            MDC.put("projectType", projectType.toString)
            val writtenPath = resultSeq(1).asInstanceOf[String]
            MDC.put("writtenPath", writtenPath)
            val workingGroupMaybe = resultSeq(2).asInstanceOf[Option[PlutoWorkingGroup]]
            MDC.put("workingGroupMaybe", workingGroupMaybe.toString)
            val commissionMaybe = resultSeq(3).asInstanceOf[Option[PlutoCommission]]
            MDC.put("commissionMaybe", commissionMaybe.toString)

            val sortedActions = createRequest.data.postrunSequence.get
            val actionResults: Seq[Try[JythonOutput]] =
              runNextAction(sortedActions, Seq(), PostrunDataCache(), writtenPath, createdProjectEntry, projectType, workingGroupMaybe, commissionMaybe)

            val actionSuccess = collectFailures[JythonOutput](actionResults)

            actionSuccess match {
              case Left(errorSeq) =>
                MDC.put("errorSeq", errorSeq.toString())
                val msg = s"${errorSeq.length} postrun actions failed for project $writtenPath, see log for details"
                val ex = new RuntimeException(msg)
                logger.error(msg)
                errorSeq.foreach(err => logger.error(s"\tMethod failed with:", err))
                originalSender ! StepFailed(createRequest.data, ex)
                Failure(ex)
              case Right(results) =>
                val scriptErrors = results.filter(_.raisedError.isDefined)
                if (scriptErrors.nonEmpty) {
                  val ex = new RuntimeException(s"${scriptErrors.length} out of ${results.length} postrun scripts failed for project $writtenPath")
                  originalSender ! StepFailed(createRequest.data, ex)
                  Failure(ex)
                } else {
                  val msg = s"Successfully ran ${results.length} postrun actions for project $writtenPath"
                  logger.info(s"Successfully ran ${results.length} postrun actions for project $writtenPath")
                  originalSender ! StepSucceded(createRequest.data)

                  val reversedResults = results.reverse

                  sendAssetFolderMessage(reversedResults, createdProjectEntry)
                  sendUpdatedUuidMessage(reversedResults, createdProjectEntry)
                  persistMetadataToDatabase(reversedResults.headOption, createdProjectEntry, results.length, writtenPath)
                  Success(msg)
                }
            }
          })
        } catch {
          case ex:Throwable=>
            originalSender ! StepFailed(createRequest.data, ex)
            Future(Success(ex.toString))  //don't replay the transaction if it failed this way
        }
      }
    case rollbackRequest:NewProjectRollback=>
      logger.debug("No rollback necessary for this step")
      sender() ! StepSucceded(rollbackRequest.data)
    case _=>
      super.receiveCommand
  }
}
