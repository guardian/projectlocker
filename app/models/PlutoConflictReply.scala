package models

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class PlutoConflictReply (project: ProjectEntry, plutoWorkingGroup: Option[PlutoWorkingGroup], possiblePlutoTypes: Seq[PlutoProjectType])

object PlutoConflictReply {
  protected def plutoTypesForProjectlockerType(projectLockerTypeId: Int)(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Seq[PlutoProjectType]] = {
    ProjectTemplate.templatesForTypeId(projectLockerTypeId).flatMap({
      case Success(projectTemplates)=>
        Future.sequence(projectTemplates.map(template=>PlutoProjectType.entryForProjectLockerTemplate(template.id.get)))
      case Failure(err)=>throw err
    }).map(_.flatten)
  }

  def getForProject(projectEntry: ProjectEntry)(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[PlutoConflictReply] = {
    val futuresList = Seq(
      Some(plutoTypesForProjectlockerType(projectEntry.projectTypeId)),
      projectEntry.workingGroupId.map(wgId=>PlutoWorkingGroup.entryForId(wgId))
    ).collect({ //filter out None values and flatten Some so we can call Future.sequence on it
      case Some(x)=>x
    })

    Future.sequence(futuresList).map(resultSeq=>{
      val maybeWorkingGroup = if(resultSeq.length>1) resultSeq(1).asInstanceOf[Option[PlutoWorkingGroup]] else None
      new PlutoConflictReply(projectEntry,maybeWorkingGroup,resultSeq.head.asInstanceOf[Seq[PlutoProjectType]])
    })
  }
}

trait PlutoConflictReplySerializer extends ProjectEntrySerializer with PlutoWorkingGroupSerializer with PlutoProjectTypeSerializer {
  implicit val plutoConflictWrites:Writes[PlutoConflictReply] = (
    (JsPath \ "project").write[ProjectEntry] and
      (JsPath \ "workingGroup").writeNullable[PlutoWorkingGroup] and
      (JsPath \ "possiblePlutoTypes").write[Seq[PlutoProjectType]]
    )(unlift(PlutoConflictReply.unapply))

  implicit val plutoConflictReads:Reads[PlutoConflictReply] = (
    (JsPath \ "project").read[ProjectEntry] and
      (JsPath \ "workingGroup").readNullable[PlutoWorkingGroup] and
      (JsPath \ "possiblePlutoTypes").read[Seq[PlutoProjectType]]
    )(PlutoConflictReply.apply _)
}
