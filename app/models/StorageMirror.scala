package models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future

case class StorageMirror (mirrorTargetStorageId:Int,mirrorSourceStorageId:Int)

object StorageMirror extends ((Int,Int)=>StorageMirror) {
  /**
    * retrieves a sequence of [[StorageMirror]] instances for each replication target of the given storage id
    * returns an empty sequence if no replication targets are present
    * the future fails if there is an error, catch this with ".recover()"
    * @param sourceStorageId storage ID to look up
    * @param db implicitly provided database pointer
    * @return a Future containing a sequence of 0 or more [[StorageMirror]] instances
    */
  def mirrorTargetsForStorage(sourceStorageId:Int)(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Seq[StorageMirror]] =
    db.run(
      TableQuery[StorageMirrorRow].filter(_.mirrorTargetSourceStorageId===sourceStorageId).result
    )
}

trait StorageMirrorSerializer {
  implicit val smReads:Reads[StorageMirror] = (
    (JsPath \ "mirrorTargetStorageId").read[Int] and
      (JsPath \ "mirrorSourceStorageId").read[Int]
  )(StorageMirror.apply _)

  implicit val smWrites:Writes[StorageMirror] = (
    (JsPath \ "mirrorTargetStorageId").write[Int] and
      (JsPath \ "mirrorSourceStorageId").write[Int]
  )(unlift(StorageMirror.unapply))
}

class StorageMirrorRow(tag:Tag) extends Table[StorageMirror](tag, "StorageMirror") {
  def mirrorTargetStorageId = column[Int]("k_mirror_target_id",O.PrimaryKey)
  def mirrorTargetSourceStorageId = column[Int]("k_mirror_source_storage_id")

  def * = (mirrorTargetStorageId, mirrorTargetSourceStorageId) <> (StorageMirror.tupled, StorageMirror.unapply)
}