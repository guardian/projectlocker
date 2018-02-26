package models
import com.sun.javafx.tools.ant.FileAssociation
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, Writes}
import slick.lifted.Query
import slick.jdbc.PostgresProfile.api._

import scala.util.Try

case class ProjectEntryFilterTerms(title:Option[String],vidispineProjectId:Option[String],filename:Option[String]) {
  /**
    * adds the relevant filter terms to the end of a Slick query
    * @param f a lambda function which is passed nothing and should return the base query to which the filter terms should
    *          be appended
    * @return slick query with the relevant filter terms added
    */
  def addFilterTerms(f: =>Query[ProjectEntryRow, ProjectEntry, Seq]):Query[ProjectEntryRow, ProjectEntry, Seq] = {
    var action = f
//    if(filename.isDefined){
    //      /* see http://slick.lightbend.com/doc/3.0.0/queries.html#joining-and-zipping */
    //      action = for {
    //        (assoc, fileEntryRow) <- TableQuery[FileAssociationRow] join TableQuery[FileEntryRow] on (_.fileEntry === _.id)
    //        (assoc, projectEntryRow) <- TableQuery[FileAssociationRow] join TableQuery[ProjectEntryRow] on (_.projectEntry === _.id) if fileEntryRow.filepath === filename.get
    //      } yield projectEntryRow
    //    }
    if(filename.isDefined){
      /* see http://slick.lightbend.com/doc/3.0.0/queries.html#joining-and-zipping */
      action = for {
        (assoc, matchingFiles) <- TableQuery[FileAssociationRow] join TableQuery[FileEntryRow] on (_.fileEntry===_.id) if matchingFiles.filepath === filename.get
        projectEntryRow <- action.filter(_.id===assoc.projectEntry)
      } yield projectEntryRow
    }

    if(title.isDefined) action = action.filter(_.projectTitle===title.get)
    if(vidispineProjectId.isDefined) action = action.filter(_.vidispineProjectId===vidispineProjectId.get)
    //if(filename.isDefined) (TableQuery[FileAssociationRow].join(action) on (_.projectEntry===_.id)).filter(_._)
    //if(filename.isDefined) action = action.join(TableQuery[FileEntryRow].filter(_.filepath === filename.get)) on
    /* filename is more problematic, as we have to then query across multiple tables */
    action
  }
}

trait ProjectEntryFilterTermsSerializer {
  implicit val projectEntryFilterTermsReads:Reads[ProjectEntryFilterTerms] = (
    (JsPath \ "title").readNullable[String] and
      (JsPath \ "vidispineId").readNullable[String] and
      (JsPath \ "filename").readNullable[String]
  )(ProjectEntryFilterTerms.apply _)
}