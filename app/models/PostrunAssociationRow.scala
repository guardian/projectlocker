package models

import slick.lifted.Tag
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future
import scala.util.Try

class PostrunAssociationRow(tag:Tag) extends Table[(Int,Int)](tag,"PostrunAssociationRow"){
  def id=column[Int]("id", O.PrimaryKey,O.AutoInc)
  def projectType=column[Int]("k_projecttype")
  def postrunEntry=column[Int]("k_postrun")

  def projectTypeFk = foreignKey("FK_PROJECT_TYPE", projectType, TableQuery[ProjectTypeRow])(_.id)
  def postrunEntryFk = foreignKey("FK_POSTRUN_ENTRY", postrunEntry, TableQuery[PostrunActionRow])(_.id)

  def * = (projectType, postrunEntry)
}

object PostrunAssociation {
  def entriesForProjectType(projectTypeId: Int)(implicit db:slick.jdbc.JdbcProfile#Backend#Database):Future[Try[Seq[(Int, Int)]]] = {
    db.run(
      TableQuery[PostrunAssociationRow].filter(_.projectType===projectTypeId).result.asTry
    )
  }
}