package models

import slick.jdbc.PostgresProfile.api._

/**
  * This table maintains assocations between files and projects, i.e. which files are part of which project
  * @param tag
  */
class FileAssociationRow(tag: Tag) extends Table[(Int,Int)](tag, "ProjectFileAssociation") {
  def id=column[Int]("id",O.PrimaryKey,O.AutoInc)
  def projectEntry = column[Int]("k_project_entry")
  def fileEntry = column[Int]("k_file_entry")

  def projectEntryFk=foreignKey("FK_PROJECT_ENTRY",projectEntry,TableQuery[ProjectEntryRow])(_.id)
  def fileEntryFk=foreignKey("FK_FILE_ENTRY",fileEntry,TableQuery[FileEntryRow])(_.id)

  def * = (projectEntry, fileEntry)
}