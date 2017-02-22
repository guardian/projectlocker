package controllers

import play.api._
import play.api.mvc.{BodyParsers, _}
import javax.inject._
import akka.actor.{Actor, ActorRef, ActorSystem}
import actors._
import akka.pattern.ask
import akka.util.Timeout
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsError, JsPath, Json, Reads}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class ProjectRequest(filename:String,projectTypeId:Int,projectTemplateId:Int){

}

@Singleton
class Application @Inject() (system: ActorSystem, @Named("project-creation-actor") projectCreationActor: ActorRef) extends Controller {
  implicit val timeout:akka.util.Timeout = Timeout(30.seconds)

  implicit val projectRequestReads:Reads[ProjectRequest] = (
      (JsPath \ "filename").read[String] and
      (JsPath \ "projectTypeId").read[Int] and
      (JsPath \ "projectTemplateId").read[Int]
    )(ProjectRequest.apply _)

  def index(path:String) = Action {
    Ok(views.html.index())
  }

  def createProject = Action.async(BodyParsers.parse.json) { request=>
    request.body.validate[ProjectRequest].fold(
      errors => {
        Future(BadRequest(Json.obj("status"->"error","detail"->JsError.toJson(errors))))
      },
      projectRequest => {
        //this will return a future that contains the id of a database record that can be used to look up progress.
        //it completes as soon as this is done, and leaves the overall processing to take place.
        val responseFuture =
          projectCreationActor ? ProjectCreationActor.CreateProject(projectRequest.filename,
            projectRequest.projectTypeId,
            projectRequest.projectTemplateId,
            None
          )

        responseFuture.map(response=>{
          val jobid = response.asInstanceOf[Int]
          Ok(s"got job id $jobid")
        })
      }
    )

  }
}