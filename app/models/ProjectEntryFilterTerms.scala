package models
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsValue, _}
import slick.lifted.Query
import slick.jdbc.PostgresProfile.api._


case class ProjectEntryFilterTerms(title:Option[String],
                                   vidispineProjectId:Option[String],
                                   filename:Option[String],
                                   wildcard:FilterTypeWildcard.Value)
extends GeneralFilterEntryTerms[ProjectEntryRow, ProjectEntry] {

  /**
    * adds the relevant filter terms to the end of a Slick query
    * @param f a lambda function which is passed nothing and should return the base query to which the filter terms should
    *          be appended
    * @return slick query with the relevant filter terms added
    */
  override def addFilterTerms(f: =>Query[ProjectEntryRow, ProjectEntry, Seq]):Query[ProjectEntryRow, ProjectEntry, Seq] = {
    var action = f
    if(filename.isDefined){
      /* see http://slick.lightbend.com/doc/3.0.0/queries.html#joining-and-zipping */
      action = for {
        (assoc, matchingFiles) <- TableQuery[FileAssociationRow] join TableQuery[FileEntryRow] on (_.fileEntry===_.id) if matchingFiles.filepath like makeWildcard(filename.get)
        projectEntryRow <- action.filter(_.id===assoc.projectEntry)
      } yield projectEntryRow
    }

    if(title.isDefined) action = action.filter(_.projectTitle like makeWildcard(title.get))
    if(vidispineProjectId.isDefined) action = action.filter(_.vidispineProjectId like makeWildcard(vidispineProjectId.get))
    action
  }
}

trait ProjectEntryFilterTermsSerializer {
  implicit val wildcardSerializer:Reads[FilterTypeWildcard.Value] = Reads.enumNameReads(FilterTypeWildcard)

  implicit val projectEntryFilterTermsReads:Reads[ProjectEntryFilterTerms] = (
    (JsPath \ "title").readNullable[String] and
      (JsPath \ "vidispineId").readNullable[String] and
      (JsPath \ "filename").readNullable[String] and
      (JsPath \ "match").read[FilterTypeWildcard.Value]
  )(ProjectEntryFilterTerms.apply _)
}