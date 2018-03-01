package models

import slick.lifted.Tag
import slick.jdbc.PostgresProfile.api._

class PostrunAssociationRow(tag:Tag) extends Table[(Int,Int)](tag,"PostrunAssociationRow"){
  def id=column[Int]("id", O.PrimaryKey,O.AutoInc)
  def projectType=column[Int]("k_projecttype")
  def postrunEntry=column[Int]("k_postrun")

  def projectTypeFk = foreignKey("FK_PROJECT_TYPE", projectType, TableQuery[ProjectTypeRow])(_.id)
  def postrunEntryFk = foreignKey("FK_POSTRUN_ENTRY", postrunEntry, TableQuery[PostrunActionRow])(_.id)

  def * = (projectType, postrunEntry)
}
