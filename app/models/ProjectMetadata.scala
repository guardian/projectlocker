package models

import play.api.Logger
import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

case class ProjectMetadata (id:Option[Int],projectRef:Int,key:String,value:Option[String]){
  val logger = Logger(getClass)

  def save(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Try[ProjectMetadata]] = id match {
    case None=>
      logger.debug("Inserting new record")
      val insertQuery = TableQuery[ProjectMetadataRow] returning TableQuery[ProjectMetadataRow].map(_.id) into ((item,id)=>item.copy(id=Some(id)))
      db.run(
        (insertQuery+=this).asTry
      ).map({
        case Success(insertResult)=>Success(insertResult)
        case Failure(error)=>Failure(error)
      })
    case Some(realEntityId)=>
      logger.debug("Updating record")
      db.run(
        TableQuery[ProjectMetadataRow].filter(_.id===realEntityId).update(this).asTry
      ).map({
        case Success(rowsAffected)=>Success(this)
        case Failure(error)=>Failure(error)
      })
  }
}

object ProjectMetadata extends ((Option[Int], Int, String, Option[String])=>ProjectMetadata){
  private val logger = Logger(getClass)
  /**
    * Gets a metadata entry for the given project
    * @param projectRef projectEntry ID
    * @param key metadata key.
    * @param db implicitly provided database object
    * @return a Future, containing an Option which has the key if necessary. Future will fail if a db error occurs
    */
  def entryFor(projectRef:Int, key:String)(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Option[ProjectMetadata]] =
    db.run(
      //unique constraint on the table ensures that there can only be zero or one responses
      TableQuery[ProjectMetadataRow].filter(_.projectRef===projectRef).filter(_.key===key).result
    ).map(_.headOption)

  def getOrCreate(projectRef:Int, key:String, autoSave:Boolean=false)(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Try[ProjectMetadata]] =
    db.run(
      //unique constraint on the table ensures that there can only be zero or one responses
      TableQuery[ProjectMetadataRow].filter(_.projectRef===projectRef).filter(_.key===key).result
    ).map(_.headOption).flatMap({
      case Some(entry)=>Future(Success(entry))
      case None=>
        val newEntry = ProjectMetadata(None, projectRef, key, None)
        if(autoSave){
          newEntry.save
        } else {
          Future(Success(newEntry))
        }
    })

  def deleteFor(projectRef: Int, key:String)(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Try[Int]] = {
    db.run(
      TableQuery[ProjectMetadataRow].filter(_.projectRef===projectRef).filter(_.key===key).delete.asTry
    )
  }

  /**
    * Gets all metadata entries for the given project
    * @param projectRef projectEntry ID
    * @param db implicitly provided database object
    * @return a Future, containing a Try containing a Sequence of ProjectMetadata objects
    */
  def allMetadataFor(projectRef:Int)(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Try[Seq[ProjectMetadata]]] =
    db.run(
      TableQuery[ProjectMetadataRow].filter(_.projectRef===projectRef).result.asTry
    )

  /**
    * Set (by upsert) entries in bulk
    * @param projectRef project ID to set entries for
    * @param data a Map[String,String] containing keys and values to set
    * @param db implicitly provided database object
    * @return a Future, containing an Int indicating the number of insert/updates. The future will fail if there is a database error
    */
  def setBulk(projectRef:Int, data:Map[String,String])(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Try[Int]] = {
    def tryInsertWithRecovery(mdEntry:ProjectMetadata,onRetry:Boolean=false):Future[ProjectMetadata] = mdEntry.save.flatMap({
      case Failure(err)=>
        val errorString = err.toString
        if (errorString.contains("violates foreign key constraint") ||
          errorString.contains("Referential integrity constraint violation") ||
          errorString.contains("violates unique constraint")) {
          ProjectMetadata.deleteFor(mdEntry.projectRef, mdEntry.key)
          if (onRetry)
            throw err
          else
            tryInsertWithRecovery(mdEntry, onRetry = true)
        } else {
          throw err
        }
      case Success(savedEntry)=>Future(savedEntry)
    })

    val objectsToSet = Future.sequence(data.map(kvTuple=>ProjectMetadata.getOrCreate(projectRef,kvTuple._1)))
    val splitResultsFuture = objectsToSet.map(_.partition(_.isSuccess))

    splitResultsFuture.flatMap(resultTuple=>{
      if(resultTuple._2.count(x=>true)>0){
        val resultSeq = resultTuple._2.foldLeft("")((acc, failedTry)=>acc + failedTry.failed.get.toString).mkString("; ")
        Future(Failure(new RuntimeException(resultSeq)) ) //fixme: define a custom exception to hold the sequence instead
      } else {
        Future.sequence(resultTuple._1.map(successfulTry=>tryInsertWithRecovery(successfulTry.get)))
          .map(iterable=>Success(iterable.count(x=>true)))
          .recover({ case ex:Throwable=>Failure(ex) })
      }
    })
  }
}

class ProjectMetadataRow(tag:Tag) extends Table[ProjectMetadata](tag,"ProjectMetadata"){
  def id=column[Int]("id",O.PrimaryKey,O.AutoInc)
  def projectRef=column[Int]("k_project_entry")
  def key=column[String]("s_key")
  def value=column[String]("s_value")

  def * = (id.?,projectRef,key,value.?) <> (ProjectMetadata.tupled, ProjectMetadata.unapply)
}

trait ProjectMetadataSerializer {
  implicit val projectMetadataWrites:Writes[ProjectMetadata] = (
    (JsPath \ "id").writeNullable[Int] and
      (JsPath \ "projectEntryRef").write[Int] and
      (JsPath \ "key").write[String] and
      (JsPath \ "value").writeNullable[String]
  )(unlift(ProjectMetadata.unapply))

  implicit val projectMetadataReads:Reads[ProjectMetadata] = (
    (JsPath \ "id").readNullable[Int] and
      (JsPath \ "projectEntryRef").read[Int] and
      (JsPath \ "key").read[String] and
      (JsPath \ "value").readNullable[String]
    )(ProjectMetadata.apply _)
}