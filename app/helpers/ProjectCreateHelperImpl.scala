package helpers

import models._
import java.sql.Timestamp

import scala.concurrent.{Await, Future}
import java.time.LocalDateTime
import javax.inject.Singleton

import exceptions.{PostrunActionError, ProjectCreationError}
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

@Singleton
class ProjectCreateHelperImpl extends ProjectCreateHelper {
  protected val storageHelper:StorageHelper = new StorageHelper
  val logger: Logger = Logger(this.getClass)

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
  def getDestFileFor(rq:ProjectRequestFull, recordTimestamp:Timestamp)(implicit db: slick.jdbc.JdbcProfile#Backend#Database): Future[Try[FileEntry]] =
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

  def orderPostruns(unodererdList:Seq[PostrunAction],dependencies:Map[Int,Seq[Int]]):Seq[PostrunAction] = unodererdList sortWith { (postrunA,postrunB)=>
    val firstTest = dependencies.get(postrunA.id.get) match {
      case Some(dependencies)=>
//        if(dependencies.contains(postrunB.id.get))
//          println(s"'${postrunA.title}' Adeps '${postrunB.title}'")
//        else
//          println(s"'${postrunA.title}' not deps '${postrunB.title}'")
        dependencies.contains(postrunB.id.get)  //if A depends on B, then reverse the order
      case None=>
//        println(s"'${postrunA.runnable}' has no dependencies")
        logger.debug(s"${postrunA.runnable} has no dependencies")
        false
    }

    !firstTest
  }

  protected def runEach(action:PostrunAction, projectFileName:String, projectEntry:ProjectEntry, dataCache: PostrunDataCache, projectType:ProjectType)
                     (implicit db: slick.jdbc.JdbcProfile#Backend#Database, config:play.api.Configuration):Try[JythonOutput] = {

    val timeout:Duration = Duration(config.getOptional[String]("postrun.timeout").getOrElse("30 seconds"))

    Await.result(action.run(projectFileName,projectEntry, projectType, dataCache),timeout)
  }

  protected def syncExecScript(action: PostrunAction, projectFileName: String, entry: ProjectEntry, projectType: ProjectType, cache: PostrunDataCache)
                    (implicit db: slick.jdbc.JdbcProfile#Backend#Database, config:play.api.Configuration, timeout: Duration) =
    Await.result(action.run(projectFileName,entry,projectType,cache), timeout)

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
                    projectFileName: String, projectEntry: ProjectEntry, projectType: ProjectType)
                   (implicit db: slick.jdbc.JdbcProfile#Backend#Database, config:play.api.Configuration, timeout: Duration):Seq[Try[JythonOutput]] = {
    logger.debug(s"runNextAction: remaining actions: ${actions.toString()}")
    actions.headOption match {
      case Some(nextAction)=>
        logger.info(s"running action ${nextAction.toString}")
        val newResults = results ++ Seq(syncExecScript(nextAction, projectFileName,projectEntry, projectType, cache))
        logger.info(s"got results: ${newResults.toString()}")
        runNextAction(actions.tail, newResults, cache, projectFileName, projectEntry, projectType)
      case None=>
        logger.info("recursion ends")
        results
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

  /**
    * Main method to execute the postrun actions for a given project type, during project creation
    * @param fileEntry [[FileEntry]] representing the created file
    * @param eventualTriedEntry a Future, containing a Try, containing the [[ProjectEntry]] that has been created
    * @param template [[ProjectTemplate]] representing the project template used to create ProjectEntry
    * @param db Implicitly provided database object
    * @param config Implicitly provided Play app configuration
    * @return a Future, containing either a Left with a string describing the number of actions that errored
    *         or a Right with a string indicating how many actions were run
    */
  def doPostrunActions(fileEntry: FileEntry, eventualTriedEntry: Future[Try[ProjectEntry]], template: ProjectTemplate)
                      (implicit db: slick.jdbc.JdbcProfile#Backend#Database, config:play.api.Configuration):Future[Either[String,String]]= {
    val futureSequence = Future.sequence(Seq(eventualTriedEntry, template.projectType, fileEntry.getFullPath, PostrunDependencyGraph.loadAllById))

    implicit val timeout:Duration = Duration(config.getOptional[String]("postrun.timeout").getOrElse("30 seconds"))
    futureSequence.flatMap(completedFutures=>{
      val projectEntryTry:Try[ProjectEntry] = completedFutures.head.asInstanceOf[Try[ProjectEntry]]
      projectEntryTry match {
        case Failure(error)=>Future(Left(error.toString))
        case Success(projectEntry)=>
          val projectType:ProjectType = completedFutures(1).asInstanceOf[ProjectType]
          val writtenPath = completedFutures(2).asInstanceOf[String]
          val postrunDependencyGraph = completedFutures(3).asInstanceOf[Map[Int, Seq[Int]]]

          val actionResults:Future[Seq[Try[JythonOutput]]] = projectType.postrunActions.map({
            case Failure(error)=>Seq(Failure(error))
            case Success(actionsList)=>
              //.map(action=>runEach(action, writtenPath, projectEntry, projectType))
              val sortedActions = orderPostruns(actionsList, postrunDependencyGraph)
              runNextAction(sortedActions, Seq(), PostrunDataCache(), writtenPath, projectEntry, projectType)
          })

          val actionSuccess = actionResults.map(collectFailures _)
          actionSuccess map {
            case Left(errorSeq)=>
              val msg = s"${errorSeq.length} postrun actions failed for project $writtenPath, see log for details"
              logger.error(msg)
              errorSeq.foreach(err=>logger.error(s"\tMethod failed with:", err))
              Left(msg)
            case Right(results)=>
              val msg = s"Successfully ran ${results.length} postrun actions for project $writtenPath"
              logger.info(s"Successfully ran ${results.length} postrun actions for project $writtenPath")
              Right(s"Successfully ran ${results.length} postrun actions for project $writtenPath")
          }
      }
    })
  }

  /**
    * Logic to create a project.  This runs asynchronously, taking in a project request in the form of a [[models.ProjectRequestFull]]
    * and copying the requested template to the final destination
    * @param rq [[ProjectRequestFull]] object representing the project request
    * @param createTime optional [[LocalDateTime]] as the create time.  If None is provided then current date/time is used
    * @param db implicitly provided [[slick.jdbc.JdbcProfile#Backend#Database]]
    * @return a [[Try]] containing a saved [[models.ProjectEntry]] object if successful, wrapped in a [[Future]]
    */
  def create(rq:ProjectRequestFull,createTime:Option[LocalDateTime])
            (implicit db: slick.jdbc.JdbcProfile#Backend#Database, config: play.api.Configuration):Future[Try[ProjectEntry]] = {
    logger.info(s"Creating project from $rq")
    rq.destinationStorage.getStorageDriver match {
      case None=>
        Future(Failure(new RuntimeException(s"Storage ${rq.destinationStorage.id} does not have any storage driver configured")))
      case Some(storageDriver)=>
        logger.info(s"Got storage driver: $storageDriver")

        val recordTimestamp = Timestamp.valueOf(createTime.getOrElse(LocalDateTime.now()))
        val futureDestFileEntry = getDestFileFor(rq, recordTimestamp)

        val savedDestFileEntry = futureDestFileEntry.flatMap({
          case Success(fileEntry)=>fileEntry.save
          case Failure(error)=>Future(Failure(error))
        })

        savedDestFileEntry flatMap {
          case Success(savedFileEntry)=>
            val fileCopyFuture=rq.projectTemplate.file.flatMap(sourceFileEntry=>{
              logger.info(s"Copying from file $sourceFileEntry to $savedFileEntry")
              storageHelper.copyFile(sourceFileEntry, savedFileEntry)
            })

            fileCopyFuture.flatMap({
              case Left(error)=>
                logger.error(s"File copy failed: ${error.toString}")
                Future(Failure(new RuntimeException(error.mkString("\n"))))
              case Right(writtenFile)=>
                logger.info(s"Creating new project entry from $writtenFile")
                val createResult = ProjectEntry.createFromFile(writtenFile, rq.projectTemplate, rq.title, createTime,rq.user)
                logger.info("Done")
                val postruns = doPostrunActions(writtenFile, createResult, rq.projectTemplate) map {
                  case Left(errorMessage)=>
                    Failure(new PostrunActionError(errorMessage))
                  case Right(successMessage)=>
                    Success(successMessage)
                }

                Future.sequence(Seq(createResult, postruns)) map { results=>
                  val finalCreateResult = results.head.asInstanceOf[Try[ProjectEntry]]
                  val finalPostrunResult = results(1).asInstanceOf[Try[String]]

                  if(finalPostrunResult.isFailure)
                    Failure(finalPostrunResult.failed.get)
                  else if(finalCreateResult.isFailure)
                    Failure(finalCreateResult.failed.get)
                  else
                    finalCreateResult
                }
            })
          case Failure(error)=>
            logger.error("Unable to save destination file entry to database", error)
            Future(Failure(error))
        }
    }
  }
}
