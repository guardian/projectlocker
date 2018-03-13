package models
import play.api.libs.json.{JsPath, JsValue, Reads, Writes}
import play.api.libs.functional.syntax._
import slick.lifted.Tag
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class PostrunDependency (id:Option[Int], sourceAction: Int, dependsOn: Int)

import scala.concurrent.ExecutionContext.Implicits.global
class PostrunDependencyRow(tag:Tag) extends Table[PostrunDependency](tag,"PostrunDependency") {
  def id=column[Int]("id", O.PrimaryKey,O.AutoInc)
  def sourceAction=column[Int]("k_source")
  def dependsOn=column[Int]("k_dependson")

  def sourceActionFk = foreignKey("FK_SOURCE", sourceAction, TableQuery[PostrunActionRow])(_.id)
  def dependsOnFk = foreignKey("FK_DEPENDS_ON", dependsOn, TableQuery[PostrunActionRow])(_.id)

  def * = (id.?, sourceAction, dependsOn) <> (PostrunDependency.tupled, PostrunDependency.unapply)
}

object PostrunDependencyGraph {
  /**
    * Loads the entire dependency graph into memory as a future, and returns it
    * @param db implicitly provided database object
    * @return a Future, containing a Map of Int (representing postrun ID) to a Seq of PostrunDependency objects
    */
  def loadAll(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Map[Int,Seq[PostrunDependency]]] =
    db.run(TableQuery[PostrunDependencyRow].result.asTry) map {
      case Failure(error)=>throw error  //fail the future if an error occurs
      case Success(rows)=>
        rows.foldLeft[Map[Int,Seq[PostrunDependency]]](Map()) { (acc,entry)=>
          acc ++ Map(entry.sourceAction->rows.filter(_.sourceAction==entry.sourceAction))
        }
    }

  /**
    * Convenience method that calls [[loadAll]] and converts the PostrunDependency object into just the id of the postrun that is depended on
    * @param db
    * @return
    */
  def loadAllById(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Map[Int,Seq[Int]]] = loadAll map { data=>
    data.mapValues(_.map(_.dependsOn))
  }
}

trait PostrunDependencySerializer {
  implicit val postrunDependencyWrites:Writes[PostrunDependency] = (
    (JsPath \ "id").writeNullable[Int] and
    (JsPath \ "sourceAction").write[Int] and
      (JsPath \ "dependsOn").write[Int]
  )(unlift(PostrunDependency.unapply))
}