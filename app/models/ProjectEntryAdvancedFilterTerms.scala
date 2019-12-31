package models

import play.api.libs.json.{JsPath, Reads}
import slick.lifted.{Query, TableQuery}
import slick.jdbc.PostgresProfile.api._
import play.api.libs.functional.syntax._

case class ProjectEntryCrossFilters(commissionId:Option[VsID], workingGroupId:Option[String])

/**
  * represents an "advanced search" on projects, which means that we can also search against commission VSIDs and
  * working group UUIDs.
  * this involves performing a three-table join in Postgres and querying against that
  * @param query regular [[ProjectEntryFilterTerms]] to search for
  * @param filter extra parameters for filtering on a commission ID and a working group ID
  */
case class ProjectEntryAdvancedFilterTerms (query:ProjectEntryFilterTerms, filter:ProjectEntryCrossFilters)
{
  /**
    * return a query against a join of the three tables in question.
    *
    * @return a Slick query for projects with a commission numeric ID matching the parts of the commissionId (if given)
    *         and with a working group numeric ID matching the provided working group UUID (if given).
    */
  def makeSearchWithJoin = {
    //make a join query for the three tables
    val joined = for {
      ((project, commission), workinggroup) <- TableQuery[ProjectEntryRow]
        .join(TableQuery[PlutoCommissionRow]).on(_.commission===_.id)
        .join(TableQuery[PlutoWorkingGroupRow]).on(_._1.workingGroup===_.id)
    } yield (project,commission, workinggroup)
    //now add in filter terms for the commission-specific data, if present; otherwise pass through the original unchanged
    val maybeWithCollectionQuery = filter.commissionId.map(vsid=>joined.filter(_._2.siteId===vsid.siteId).filter(_._2.collectionId===vsid.numericId)).getOrElse(joined)
    //now add in filter terms for the working-group specific data, if present; otherwise pass through the original unchanged
    val maybeWithWgQuery = filter.workingGroupId.map(wgUuid=>maybeWithCollectionQuery.filter(_._3.uuid===wgUuid)).getOrElse(maybeWithCollectionQuery)
    query.addFilterTerms{maybeWithWgQuery.map(_._1)}
  }


  /**
    * return a query against a join of the three tables in question.
    * @param f a lambda function which is passed nothing and should return the base query to which the filter terms should
    *          be appended
    * @return a Slick query for projects with a commission numeric ID matching the parts of the commissionId (if given)
    *         and with a working group numeric ID matching the provided working group UUID (if given).
    */
  def addFilterTerms(f: =>Query[ProjectEntryRow,ProjectEntry,Seq]) = {
    //make a join query for the three tables
    val joined = for {
      ((project, commission), workinggroup) <- query.addFilterTerms(f)
        .join(TableQuery[PlutoCommissionRow]).on(_.commission===_.id)
        .join(TableQuery[PlutoWorkingGroupRow]).on(_._1.workingGroup===_.id)
    } yield (project,commission, workinggroup)
    //now add in filter terms for the commission-specific data, if present; otherwise pass through the original unchanged
    val maybeWithCollectionQuery = filter.commissionId.map(vsid=>joined.filter(_._2.siteId===vsid.siteId).filter(_._2.collectionId===vsid.numericId)).getOrElse(joined)
    //now add in filter terms for the working-group specific data, if present; otherwise pass through the original unchanged
    val maybeWithWgQuery = filter.workingGroupId.map(wgUuid=>maybeWithCollectionQuery.filter(_._3.uuid===wgUuid)).getOrElse(maybeWithCollectionQuery)
    maybeWithWgQuery
  }
}

trait ProjectEntryAdvancedFilterTermsSerializer extends ProjectEntryFilterTermsSerializer with VsIDSerializer {
  implicit val projectEntryCrossFiltersReads:Reads[ProjectEntryCrossFilters] = (
    (JsPath \ "commissionId").readNullable[VsID] and
      (JsPath \ "workingGroupId").readNullable[String]
  )(ProjectEntryCrossFilters.apply _)

  implicit val projectEntryAdvancedFilterTermsReads:Reads[ProjectEntryAdvancedFilterTerms] = (
    (JsPath \ "query").read[ProjectEntryFilterTerms] and
      (JsPath \ "filter").read[ProjectEntryCrossFilters]
    )(ProjectEntryAdvancedFilterTerms.apply _)
}