package controllers

import javax.inject.{Inject, Singleton}

import play.api._
import play.api.mvc._
import auth.{LDAP, Security, User}
import com.unboundid.ldap.sdk.LDAPConnectionPool
import models.{LoginRequest, LoginRequestSerializer}
import play.api.cache.SyncCacheApi
import play.api.libs.json._

import scala.util.{Failure, Success}

@Singleton
class Application @Inject() (cc:ControllerComponents, p:PlayBodyParsers, cacheImpl:SyncCacheApi)
  extends AbstractController(cc) with Security with LoginRequestSerializer {

  implicit val cache:SyncCacheApi = cacheImpl

  /**
    * Action to provide base html and frontend code to the client
    * @param path http path postfix, not used but must be included to allow an indefinite path in routes
    * @return Action containing html
    */
  def index(path:String) = Action {
    Ok(views.html.index())
  }

  /**
    * Action to allow the client to authenticate.  Expects a JSON body containing username and password (use https!!!)
    * @return If login is successful, a 200 response containing a session cookie that authenticates the user.
    *         If unsuccessful, a 403 response
    *         If the data is malformed, a 400 response
    *         If an error occurs, a 500 response with a basic error message directing the user to go to the logs
    */
  def authenticate = Action(p.json) { request=>
    LDAP.connectionPool.fold(
      errors=> {
        logger.error("LDAP not configured properly", errors)
        InternalServerError(Json.obj("status" -> "error", "detail" -> "ldap not configured properly, see logs"))
      },
      ldapConnectionPool=> {
        implicit val pool: LDAPConnectionPool = ldapConnectionPool
        request.body.validate[LoginRequest].fold(
          errors => {
            BadRequest(Json.obj("status" -> "error", "detail" -> JsError.toJson(errors)))
          },
          loginRequest => {
            User.authenticate(loginRequest.username, loginRequest.password) match {
              case Success(Some(user)) =>
                Ok(Json.obj("status" -> "ok", "detail" -> "Logged in", "uid" -> user.uid)).withSession("uid" -> user.uid)
              case Success(None) =>
                logger.warn(s"Failed login from ${loginRequest.username} with password ${loginRequest.password} from host ${request.host}")
                Forbidden(Json.obj("status" -> "error", "detail" -> "forbidden"))
              case Failure(error) =>
                logger.error(s"Authentication error when trying to log in ${loginRequest.username}. This could just mean a wrong password.", error)
                Forbidden(Json.obj("status" -> "error", "detail" -> "forbidden"))
            }
          })
      }
    )
  }

  /**
    * Action that allows the frontend to test if the current session is valid
    * @return If the session is not valid, a 403 response
    *         If the session is valid, a 200 response with the currently logged in userid in a json object
    */
  def isLoggedIn = IsAuthenticated { uid=> { request=>
    Ok(Json.obj("status"->"ok","uid"->uid))
  }}

  /**
    * Action to log out, by clearing the client's session cookie.
    * @return
    */
  def logout = Action { request=>
    Ok(Json.obj("status"->"ok","detail"->"Logged out")).withNewSession
  }
}