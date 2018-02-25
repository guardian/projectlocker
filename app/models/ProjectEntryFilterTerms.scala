package models
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
    if(title.isDefined) action = action.filter(_.projectTitle===title.get)
    if(vidispineProjectId.isDefined) action = action.filter(_.vidispineProjectId===vidispineProjectId.get)
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