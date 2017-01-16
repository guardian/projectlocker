package models

import slick.driver.PostgresDriver.api._
import org.joda.time.DateTime
import java.sql.Timestamp
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC

case class ProjectEntry (files: List[FileEntry], projectType: ProjectType, created:DateTime, user: String) {

}

class ProjectFileAssocationRow(tag: Tag) extends Table[(Int)](tag, "ProjectFileAssociation") {
  def id=column[Int]("id",O.PrimaryKey,O.AutoInc)
  def projectEntry=foreignKey("ProjectEntry","id",TableQuery[ProjectEntry])
  def fileEntry=foreignKey("FileEntry","id",TableQuery[FileEntry])

  def * = id
}

class ProjectEntryRow(tag:Tag) extends Table[ProjectEntry](tag, "ProjectEntry") {
//  implicit val DateTimeMapper: TypeMapper[DateTime] =
//    base[DateTime, Timestamp](
//      d => new Timestamp(d millis),
//      t => new DateTime(t getTime, UTC)
//    )
  implicit val myIDColumnType =
    MappedColumnType.base[DateTime, Timestamp](d=>new Timestamp(d.millis), new DateTime(_.getTime, UTC))

  def id=column[Int]("id",O.PrimaryKey,O.AutoInc)
  def fileAssociationKey=foreignKey("ProjectFileAssociation","id",TableQuery[ProjectFileAssocationRow])
  def projectType=foreignKey("ProjectType","id",TableQuery[ProjectType])
  def created=column[DateTime]("created")
  def user=column[String]("user")
}