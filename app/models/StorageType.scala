package models
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, Writes}

trait StorageTypeSerializer {
  implicit val storageTypeWrites:Writes[StorageType] = (
    (JsPath \ "name").write[String] and
      (JsPath \ "needsLogin").write[Boolean] and
      (JsPath \ "hasSubFolders").write[Boolean]
    )(unlift(StorageType.unapply))

  implicit val storageTypeReads:Reads[StorageType] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "needsLogin").read[Boolean] and
      (JsPath \ "hasSubFolders").read[Boolean]
  )(StorageType.apply _)
}

case class StorageType(name:String, needsLogin: Boolean, hasSubfolders: Boolean) {

}
