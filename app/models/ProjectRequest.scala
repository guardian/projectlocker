package models

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsError, JsPath, Json, Reads}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class ProjectRequest(filename:String,destinationStorageId:Int,projectTemplateId:Int) {
  /* looks up the ids of destination storage and project template, and returns a new object with references to them or None */
  def hydrate(implicit db:slick.driver.JdbcProfile#Backend#Database):Future[Option[ProjectRequestFull]] = {
    val storageFuture = StorageEntryHelper.entryFor(this.destinationStorageId)
    val projectTemplateFuture = ProjectTemplateHelper.entryFor(this.projectTemplateId)

    Future.sequence(Seq(storageFuture, projectTemplateFuture)).map(resultSeq=>{
      val successfulResults = resultSeq.flatten
      if(successfulResults.length==2){
        Some(ProjectRequestFull(this.filename, successfulResults.head.asInstanceOf[StorageEntry], successfulResults(1).asInstanceOf[ProjectTemplate]))
      } else None
    })
  }
}

case class ProjectRequestFull(filename:String,destinationStorage:StorageEntry,projectTemplate:ProjectTemplate) {

}

trait ProjectRequestSerializer {
  implicit val projectRequestReads:Reads[ProjectRequest] = (
    (JsPath \ "filename").read[String] and
      (JsPath \ "destinationStorageId").read[Int] and
      (JsPath \ "projectTemplateId").read[Int]
  )(ProjectRequest.apply _)
}
