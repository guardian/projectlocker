package models
import slick.lifted.Tag
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future
import scala.util.Try

case class PostrunDependency (id:Option[Int], sourceAction: Int, dependensOn: Int)

class PostrunDependencyRow(tag:Tag) extends Table[PostrunDependency](tag,"PostrunDependency") {
  def id=column[Int]("id", O.PrimaryKey,O.AutoInc)
  def sourceAction=column[Int]("k_source")
  def dependsOn=column[Int]("k_dependson")

  def sourceActioneFk = foreignKey("FK_SOURCE", sourceAction, TableQuery[PostrunActionRow])(_.id)
  def dependsOnFk = foreignKey("FK_DEPENDS_ON", dependsOn, TableQuery[PostrunActionRow])(_.id)

  def * = (id.?, sourceAction, dependsOn) <> (PostrunDependency.tupled, PostrunDependency.unapply)
}

