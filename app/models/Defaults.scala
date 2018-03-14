package models

import java.io.FileInputStream
import java.nio.file.Paths
import slick.jdbc.PostgresProfile.api._
import java.sql.Timestamp

import drivers.StorageDriver
import play.api.Logger
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc.{RawBuffer, Result}
import slick.lifted.TableQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}
import scala.concurrent.{Await, Future}

case class Defaults(id:Option[Int],name:String,value:String) {
  def toInt:Int = value.toInt

  def save(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Try[Defaults]] = id match {
    case None=> //no existing id means that we should insert

  }
}

class DefaultsRow(tag:Tag) extends Table[Defaults](tag,"defaults") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("s_name")
  def value = column[String]("s_value")

  def * = (id.?, name, value) <> (Defaults.tupled, Defaults.unapply)
}

object Defaults extends ((Option[Int],String,String)=>Defaults) {
  def apply(id:Option[Int],name:String, intValue:Int):Defaults = new Defaults(id,name,intValue.toString)

  def entryFor(name:String)(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Try[Option[Defaults]]] =
    db.run(
      TableQuery[DefaultsRow].filter(_.name===name).result.asTry
    ).map({
      case Success(list)=>Success(list.headOption)
      case Failure(error)=>Failure(error)
    })
}