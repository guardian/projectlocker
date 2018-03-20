package models.messages

import models.ProjectEntry
import play.api.libs.json.{JsPath, Reads, Writes}
import play.api.libs.functional.syntax._

case class NewAssetFolder(assetFolderPath: String, projectLockerProjectId: Option[Int], plutoProjectId: Option[String]) extends QueuedMessage

object NewAssetFolder extends ((String, Option[Int], Option[String])=>NewAssetFolder){
  def forCreatedProject(assetFolderPath: String, projectEntry: ProjectEntry):Either[String, NewAssetFolder] ={
    projectEntry.vidispineProjectId match {
      case Some(plutoProjectId) =>
        Right(new NewAssetFolder(assetFolderPath, projectEntry.id, Some(plutoProjectId)))
      case None =>
        Left(s"Project ${projectEntry.id} has no pluto project ID yet")
    }
  }
}

trait NewAssetFolderSerializer {
  implicit val newAssetFolderWrites:Writes[models.messages.NewAssetFolder] = (
  (JsPath \ "assetFolderPath").write[String] and
    (JsPath \ "projectLockerProjectId").writeNullable[Int] and
    (JsPath \ "plutoProjectId").writeNullable[String]
  )(unlift(NewAssetFolder.unapply))

  implicit val newAssetFolderReads:Reads[models.messages.NewAssetFolder] = (
    (JsPath \ "assetFolderPath").read[String] and
      (JsPath \ "projectLockerProjectId").readNullable[Int] and
      (JsPath \ "plutoProjectId").readNullable[String]
  )(NewAssetFolder.apply _)
}