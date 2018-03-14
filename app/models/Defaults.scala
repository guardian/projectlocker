package models

import slick.jdbc.PostgresProfile.api._
import play.api.Logger
import play.api.libs.functional.syntax._
import play.api.libs.json._
import slick.lifted.TableQuery

import scala.util.{Failure, Success, Try}
import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

case class Defaults(id:Option[Int],name:String,value:String) {
  val logger: Logger = Logger(getClass)
  def toInt:Int = value.toInt

  def save(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Try[Defaults]] = id match {
    case None=> //no existing id means that we should insert
      val insertQuery = TableQuery[DefaultsRow] returning TableQuery[DefaultsRow].map(_.id) into ((item,id)=>item.copy(id=Some(id)))
      db.run(
        (insertQuery+=this).asTry
      ).map({
        case Success(insertResult)=>Success(insertResult.asInstanceOf[Defaults])  //maybe only intellij needs the cast here?
        case Failure(error)=>Failure(error)
      })
    case Some(realEntityId)=>
      db.run(
        TableQuery[DefaultsRow].filter(_.id===realEntityId).update(this).asTry
      ).map({
        case Success(rowsAffected)=>Success(this)
        case Failure(error)=>Failure(error)
      })
  }

  /**
    * tries to delete this record from the database
    * @param db implicitly provided database pointer
    * @return a Future, containing a Try indicating whether the operation succeeded or not, containing an Int indicating
    *         the number of deleted rows (0 or 1)
    */
  def delete(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Try[Int]] = id match {
    case None=>Future(Failure(new RuntimeException("Can't delete a record that has not been saved")))
    case Some(realEntityId)=>
      db.run(
        TableQuery[DefaultsRow].filter(_.id===realEntityId).delete.asTry
      )
  }
}

class DefaultsRow(tag:Tag) extends Table[Defaults](tag,"defaults") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("s_name")
  def value = column[String]("s_value")

  def * = (id.?, name, value) <> (Defaults.tupled, Defaults.unapply)
}

object Defaults extends ((Option[Int],String,String)=>Defaults) {
//  def apply(id:Option[Int],name:String, value:String):Defaults = new Defaults(id,name,value)
//  def apply(id:Option[Int],name:String, intValue:Int):Defaults = new Defaults(id,name,intValue.toString)

  def entryFor(name:String)(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Try[Option[Defaults]]] =
    db.run(
      TableQuery[DefaultsRow].filter(_.name===name).result.asTry
    ).map({
      case Success(list)=>Success(list.headOption)
      case Failure(error)=>Failure(error)
    })

  def allEntries(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Try[Seq[Defaults]]] =
    db.run(
      TableQuery[DefaultsRow].result.asTry
    )
}

trait DefaultsSerializer {
  implicit val defaultsWrites:Writes[Defaults] = (
    (JsPath \ "id").writeNullable[Int] and
    (JsPath \ "name").write[String] and
      (JsPath \ "value").write[String]
  )(unlift(Defaults.unapply))

  implicit val defaultsReads:Reads[Defaults] = (
    (JsPath \ "id").readNullable[Int] and
      (JsPath \ "name").read[String] and
      (JsPath \ "value").read[String]
  )(Defaults.apply _)
}