package controllers

import javax.inject.{Inject, Singleton}

import play.api._
import play.api.mvc._
import auth.{Security, User}
import models.{LoginRequest, LoginRequestSerializer}
import play.api.cache.SyncCacheApi
import play.api.libs.json._

import scala.util.{Failure, Success}

@Singleton
class Application @Inject() (cc:ControllerComponents, p:PlayBodyParsers, cacheImpl:SyncCacheApi) extends AbstractController(cc)
  with Security with LoginRequestSerializer {

  implicit val cache:SyncCacheApi = cacheImpl

  def index(path:String) = Action {
    Ok(views.html.index())
  }

  def authenticate = Action(p.json) { request=>
    request.body.validate[LoginRequest].fold(
      errors=>{
        BadRequest(Json.obj("status"->"error","detail"->JsError.toJson(errors)))
      },
      loginRequest => {
        User.authenticate(loginRequest.username, loginRequest.password) match {
          case Success(Some(user))=>
            Ok(Json.obj("status"->"ok","detail"->"Logged in")).withSession("uid"->user.uid)
          case Success(None)=>
            Logger.warn(s"Failed login from ${loginRequest.username} with password ${loginRequest.password} from host ${request.host}")
            Forbidden(Json.obj("status"->"error","detail"->"forbidden"))
          case Failure(error)=>
            Logger.error(s"Authentication error when trying to log in ${loginRequest.username}. This could just mean a wrong password.",error)
            Forbidden(Json.obj("status"->"error","detail"->"forbidden"))
        }

      })
  }
}