package models

import java.io.File
import java.nio.file.{Files, Path, Paths}
import java.sql.Timestamp

import helpers.{JythonOutput, JythonRunner, PostrunDataCache}
import org.apache.commons.io.{FileUtils, FilenameUtils}
import play.api.{Configuration, Logger}
import play.api.libs.json.{JsPath, Reads, Writes}
import play.api.libs.functional.syntax._
import postrun.PojoPostrun
import slick.lifted.{TableQuery, Tag}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

case class PostrunAction (id:Option[Int],runnable:String, title:String, description:Option[String],
                          owner:String, version:Int, ctime: Timestamp) {
  /**
    *  writes this model into the database, inserting if id is None and returning a fresh object with id set. If an id
    * was set, then returns the same object. */
  def save(implicit db: slick.jdbc.PostgresProfile#Backend#Database):Future[Try[PostrunAction]] = id match {
    case None=>
      val insertQuery = TableQuery[PostrunActionRow] returning TableQuery[PostrunActionRow].map(_.id) into ((item,id)=>item.copy(id=Some(id)))
      db.run(
        (insertQuery+=this).asTry
      ).map({
        case Success(insertResult)=>Success(insertResult.asInstanceOf[PostrunAction])  //maybe only intellij needs the cast here?
        case Failure(error)=>Failure(error)
      })
    case Some(realEntityId)=>
      db.run(
        TableQuery[PostrunActionRow].filter(_.id===realEntityId).update(this).asTry
      ).map({
        case Success(rowsAffected)=>Success(this)
        case Failure(error)=>Failure(error)
      })
  }
  /**
    * asynchronously creates a backup of the given project file as a temp file
    * @param projectFileName project file to back up
    * @return a Future, containing a Try that contains either the backup file created or an error indicating why it was not created
    */
  def backupProjectFile(projectFileName: String): Future[Try[Path]] = Future {
    Try {
      val projectNameOnly = FilenameUtils.getBaseName(projectFileName)
      val outputPath = Files.createTempFile(projectNameOnly, ".bak")

      FileUtils.copyFile(new File(projectFileName), new File(outputPath.toString))
      outputPath
    }
  }

  /**
    * synchronously copies the given backup file back over the original file
    * @param backupPath Path representing the backup file
    * @param originalFile String representing the file path to copy it over
    * @return a Try indicating whether the operation succeeded or not
    */
  def restoreBackupFile(backupPath: Path, originalFile: String) = Try {
    val logger = Logger(this.getClass)
    logger.warn(s"Restoring backup file ${backupPath.toString}")
    FileUtils.copyFile(new File(backupPath.toString), new File(originalFile))
  }

  /**
    * returns the on-disk path for the executable script
    */
  def getScriptPath(implicit config:Configuration) = Paths.get(config.get[String]("postrun.scriptsPath"), this.runnable)

  /**
    * Runs the provided python script as a postrun
    * @param projectFileName
    * @param projectEntry
    * @param projectType
    * @param dataCache
    * @param workingGroupMaybe
    * @param commissionMaybe
    * @param config
    * @return
    */
  protected def runJython(projectFileName:String,projectEntry:ProjectEntry,projectType:ProjectType,dataCache:PostrunDataCache,
                          workingGroupMaybe: Option[PlutoWorkingGroup], commissionMaybe: Option[PlutoCommission])
                         (implicit config:Configuration):Future[Try[JythonOutput]] = {
    val runner = new JythonRunner
    val inputPath = getScriptPath
    val scriptArgs = Map(
      "projectFile" -> projectFileName
    ) ++ projectEntry.asStringMap ++ projectType.asStringMap ++ commissionMaybe.map(_.asStringMap).getOrElse(Map()) ++ workingGroupMaybe.map(_.asStringMap).getOrElse(Map())

    runner.runScriptAsync(inputPath.toString, scriptArgs, dataCache) map {
      case Success(scriptOutput) =>
        val logger = Logger(this.getClass)
        logger.debug("Script started successfully")
        scriptOutput.raisedError match {
          case Some(error)=>
            logger.error("Postrun script could not complete due to a Python exception: ")
            logger.error("Postrun standard out:" + scriptOutput.stdOutContents)
            logger.error("Postrun standard err:" + scriptOutput.stdErrContents)
            Failure(error)
          case None=>
            Success(scriptOutput)
        }
      case Failure(error)=>Failure(error)
    }
  }

  /**
    * Runs the provided java class as a postrun
    * @param projectFileName
    * @param projectEntry
    * @param projectType
    * @param dataCache
    * @param workingGroupMaybe
    * @param commissionMaybe
    * @param config
    * @return
    */
  protected def runPojo(projectFileName:String,projectEntry:ProjectEntry,projectType:ProjectType,dataCache:PostrunDataCache,
                        workingGroupMaybe: Option[PlutoWorkingGroup], commissionMaybe: Option[PlutoCommission])
                       (implicit config:Configuration):Future[Try[JythonOutput]] = {
    val className: String = runnable.substring(5) //strip off "java:" prefix
    val logger = Logger(this.getClass)
    logger.debug(s"Initiating java based postrun $className...")
    try {
      val postrunClass = Class.forName(className).newInstance().asInstanceOf[PojoPostrun]

      postrunClass.postrun(projectFileName, projectEntry, projectType, dataCache, workingGroupMaybe, commissionMaybe).map({
        case Success(newDataCache) => Success(JythonOutput("", "", newDataCache, raisedError = None))
        case Failure(error) => Success(JythonOutput("", "", dataCache, raisedError = Some(error)))
      }).recoverWith({
        case ex: Throwable => Future(Failure(ex))
      })
    } catch {
      //return a failure if we couldn't initialise the classs
      case ex:Throwable=>Future(Failure(ex))
    }
  }

  /**
    * returns true of this postrun is a java object (Plain Old Java Object => POJO) or False if it's python
    */
  def isPojo:Boolean = runnable.startsWith("java:")

  /**
    * asynchronously executes this postrun action on a newly created project
    * @param projectFileName - filename of the newly created project
    * @param projectEntry - models.projectEntry object representing the newly created project
    * @param projectType - models.projectType that the project was created from
    * @param config - implicitly provided play.api.Configuration object, representing the app configuration
    * @return a Future containing a Try containing either the script output or an error
    */
  def run(projectFileName:String,projectEntry:ProjectEntry,projectType:ProjectType,dataCache:PostrunDataCache,
          workingGroupMaybe: Option[PlutoWorkingGroup], commissionMaybe: Option[PlutoCommission])
         (implicit config:Configuration):Future[Try[JythonOutput]] = {

    backupProjectFile(projectFileName) flatMap {
      case Failure(error) =>
        val logger = Logger(this.getClass)
        logger.error(s"Unable to back up project file $projectFileName:", error)
        Future(Failure(error))
      case Success(backupPath) =>
        val logger = Logger(this.getClass)
        logger.info(s"Backed up project file from $projectFileName to ${backupPath.toString}")
        logger.debug(s"Going to try to run script at path $getScriptPath...")

        val resultFuture = if(isPojo){
          runPojo(projectFileName, projectEntry, projectType, dataCache, workingGroupMaybe, commissionMaybe)
        } else {
          runJython(projectFileName, projectEntry, projectType, dataCache, workingGroupMaybe, commissionMaybe)
        }

        resultFuture.map({
          case Failure(error) =>
            logger.error("Unable to start postrun script: ", error)
            restoreBackupFile(backupPath, projectFileName) match {
              case Failure(restoreError)=>
                logger.error(s"Cannot restore backup, project file $projectFileName may be corrupted")
                Failure(error)
              case Success(unitval)=>
                Failure(error)
            }
          case Success(result)=>Success(result)
        })
    }
  }
}

object PostrunAction extends ((Option[Int],String,String,Option[String],String,Int,Timestamp)=>PostrunAction) {
  def entryForRunnable(scriptName:String)(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Try[Seq[PostrunAction]]] =
    db.run(
      TableQuery[PostrunActionRow].filter(_.runnable===scriptName).result.asTry
    )

  def allEntries(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Try[Seq[PostrunAction]]] =
    db.run(
      TableQuery[PostrunActionRow].result.asTry
    )

  def allPython(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Try[Seq[PostrunAction]]] =
    db.run(
      TableQuery[PostrunActionRow].filter(_.runnable like "%.py").result.asTry
    )
}

class PostrunActionRow(tag:Tag) extends Table[PostrunAction](tag, "PostrunAction") {
  def id = column[Int]("id",O.PrimaryKey, O.AutoInc)
  def runnable = column[String]("s_runnable")
  def title = column[String]("s_title")
  def description = column[Option[String]]("s_description")
  def owner = column[String]("s_owner")
  def version = column[Int]("i_version")
  def ctime = column[Timestamp]("t_ctime")

  def * = (id.?, runnable, title, description, owner, version, ctime) <> (PostrunAction.tupled, PostrunAction.unapply)
}

trait PostrunActionSerializer extends TimestampSerialization {
  implicit val postrunActionWrites:Writes[PostrunAction] = (
    (JsPath \ "id").writeNullable[Int] and
    (JsPath \ "runnable").write[String] and
      (JsPath \ "title").write[String] and
      (JsPath \ "description").writeNullable[String] and
      (JsPath \ "owner").write[String] and
      (JsPath \ "version").write[Int] and
      (JsPath \ "ctime").write[Timestamp]
  )(unlift(PostrunAction.unapply))

  implicit val postrunActionReads:Reads[PostrunAction] = (
    (JsPath \ "id").readNullable[Int] and
    (JsPath \ "runnable").read[String] and
      (JsPath \ "title").read[String] and
      (JsPath \ "description").readNullable[String] and
      (JsPath \ "owner").read[String] and
      (JsPath \ "version").read[Int] and
      (JsPath \ "ctime").read[Timestamp]
  )(PostrunAction.apply _)
}
