package models
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsValue, _}
import slick.lifted.Query
import slick.jdbc.PostgresProfile.api._

case class FileEntryFilterTerms(filePath:Option[String],storageId:Option[Int],user:Option[String],
                                hasContent: Option[Boolean], hasLink: Option[Boolean], wildcard:FilterTypeWildcard.Value)
  extends GeneralFilterEntryTerms[FileEntryRow, FileEntry] {

  override def addFilterTerms(f: => Query[FileEntryRow, FileEntry, Seq]):Query[FileEntryRow, FileEntry, Seq] = {
    var action = f

    if(filePath.isDefined) action = action.filter(_.filepath like makeWildcard(filePath.get))
    if(storageId.isDefined) action = action.filter(_.storage ===storageId.get)
    if(user.isDefined) action = action.filter(_.user like makeWildcard(user.get))
    if(hasContent.isDefined) action = action.filter(_.hasContent===hasContent.get)
    if(hasLink.isDefined) action = action.filter(_.hasLink===hasLink.get)
    action
  }
}

trait FileEntryFilterTermsSerializer {
  implicit val wildcardSerializer:Reads[FilterTypeWildcard.Value] = Reads.enumNameReads(FilterTypeWildcard)

  implicit val fileEntryFilterTermsReads:Reads[FileEntryFilterTerms] = (
    (JsPath \ "filePath").readNullable[String] and
      (JsPath \ "storageId").readNullable[Int] and
      (JsPath \ "user").readNullable[String] and
      (JsPath \ "hasContent").readNullable[Boolean] and
      (JsPath \ "hasLink").readNullable[Boolean] and
      (JsPath \ "match").read[FilterTypeWildcard.Value]
    )(FileEntryFilterTerms.apply _)
}