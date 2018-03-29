package models

import play.api.Logger
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsError, JsPath, Json, Reads}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

//need to pick up default storage
//need to map the right project template id - how? maybe have a defaults option for each pluto project type?

/**
  * this model represents a request from Pluto to create a project
  * @param filename filename to create with
  * @param title title of the project
  * @param plutoProjectTypeName pluto project type uuid. This must correspond to an instance of [[PlutoProjectType]] which identifes the template to use
  * @param user user that initiated the operation
  * @param workingGroupUuid uuid of the working group that this project will belong to
  * @param commissionVSID vidispine/pluto ID of the commission that this project will belong to
  */
case class ProjectRequestPluto(filename:String,title:String, plutoProjectTypeName:String,
                          user:String, workingGroupUuid: String, commissionVSID: String, vidispineId: String) {
  private val logger = Logger(getClass)
  /* looks up the ids of destination storage and project template, and returns a new object with references to them or None */
  def hydrate(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Either[Seq[String],ProjectRequestFull]] = {
    val storageFuture = StorageEntryHelper.defaultProjectfileStorage.map({
      case Some(storageEntry)=>Right(storageEntry)
      case None=>Left("No default project file storage has been set")
    })
    val projectTemplateFuture = ProjectTemplate.defaultEntryFor(plutoProjectTypeName)
    val workingGroupFuture = PlutoWorkingGroup.entryForUuid(workingGroupUuid).map({
      case Some(workingGroup)=>Right(workingGroup)
      case None=>Left(s"No working group could be found for $workingGroupUuid")
    })
    val commissionFuture = PlutoCommission.entryForVsid(commissionVSID).map({
      case Some(commission)=>Right(commission)
      case None=>Left(s"No commission could be found for $commissionVSID")
    })

    storageFuture.onComplete({
      case Success(s)=>logger.debug(s"Got storage future: $s")
      case Failure(error)=>logger.error("Could not get storage future: ", error)
    })
    projectTemplateFuture.onComplete({
      case Success(s)=>logger.debug(s"Got projectTemplateFuture: $s")
      case Failure(error)=>logger.error("Could not get projectTemplateFuture: ", error)
    })
    workingGroupFuture.onComplete({
      case Success(s)=>logger.debug(s"Got workingGroupFuture: $s")
      case Failure(error)=>logger.error("Could not get workingGroupFuture: ", error)
    })
    commissionFuture.onComplete({
      case Success(s)=>logger.debug(s"got commission: $s")
      case Failure(error)=>logger.error("Could not get commission: ", error)
    })

    Future.sequence(Seq(storageFuture, projectTemplateFuture, workingGroupFuture, commissionFuture)).map(resultSeq=>{
      val successfulResults = resultSeq.collect({
        case Right(successfulResult)=>successfulResult
      })

      if(successfulResults.length==4){
        Right(ProjectRequestFull(this.filename,
          successfulResults.head.asInstanceOf[StorageEntry],
          this.title,
          successfulResults(1).asInstanceOf[ProjectTemplate],
          user,
          successfulResults(2).asInstanceOf[PlutoWorkingGroup].id,
          successfulResults(3).asInstanceOf[PlutoCommission].id,
          existingVidispineId = Some(vidispineId),
          shouldNotify = false))
      } else {
        Left(resultSeq.collect({case Left(error)=>error}))
      }
    })
  }
}

trait ProjectRequestPlutoSerializer {
  implicit val projectRequestPlutoReads:Reads[ProjectRequestPluto] = (
    (JsPath \ "filename").read[String] and
      (JsPath \ "title").read[String] and
      (JsPath \ "plutoProjectTypeUuid").read[String] and
      (JsPath \ "user").read[String] and
      (JsPath \ "workingGroupUuid").read[String] and
      (JsPath \ "commissionVSID").read[String] and
      (JsPath \ "vidispineId").read[String]
    )(ProjectRequestPluto.apply _)
}
