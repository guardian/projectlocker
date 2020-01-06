package controllers

import java.io.FileInputStream
import java.util.Properties

import javax.inject.{Inject, Singleton}
import play.api._
import play.api.mvc._
import auth.{LDAP, Security, User}
import com.unboundid.ldap.sdk.LDAPConnectionPool
import helpers.DatabaseHelper
import models.{LoginRequest, LoginRequestSerializer}
import play.api.cache.SyncCacheApi
import play.api.http.HttpEntity
import play.api.libs.json._

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class Application @Inject() (cc:ControllerComponents, p:PlayBodyParsers, config:Configuration, cacheImpl:SyncCacheApi, dbHelper:DatabaseHelper)
  extends AbstractController(cc) with Security with LoginRequestSerializer {

  implicit val cache:SyncCacheApi = cacheImpl

  /**
    * Action to provide base html and frontend code to the client
    * @param path http path postfix, not used but must be included to allow an indefinite path in routes
    * @return Action containing html
    */
  def index(path:String) = Action {
    val cbVersionString = try {
      val prop = new Properties()
      prop.load(getClass.getClassLoader.getResourceAsStream("version.properties"))
      Option(prop.getProperty("build-sha"))
    } catch {
      case e:Throwable=>
        logger.warn("Could not get build-sha property: ", e)
        None
    }
    Ok(views.html.index(cbVersionString.getOrElse("none")))
  }

  def timeoutTest(delay: Int) = Action {
    Thread.sleep(delay*1000)
    Ok(Json.obj("status"->"ok","delay"->(delay*1000)))
  }

  /**
    * Action to allow the client to authenticate.  Expects a JSON body containing username and password (use https!!!)
    * @return If login is successful, a 200 response containing a session cookie that authenticates the user.
    *         If unsuccessful, a 403 response
    *         If the data is malformed, a 400 response
    *         If an error occurs, a 500 response with a basic error message directing the user to go to the logs
    */
  def authenticate = Action(p.json) { request=>
    val adminRoles = config.getStringList("ldap.admin-groups").map(_.asScala).getOrElse(List("Administrator"))

    logger.info(s"Admin roles are: $adminRoles")
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
                Ok(Json.obj("status" -> "ok", "detail" -> "Logged in", "uid" -> user.uid, "isAdmin"->checkRole(user.uid, adminRoles))).withSession("uid" -> user.uid)
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
    * respond to CORS options requests for login from vaultdoor
    * see https://developer.mozilla.org/en-US/docs/Glossary/Preflight_request
    * @return
    */
  def authenticateOptions = Action {
    val returnHeaders = Map(
      "Access-Control-Allow-Methods" -> "POST, OPTIONS",
      "Access-Control-Allow-Origin" -> "*"  //FIXME: only for testing. replace with a whitelist in the config
      "Access-Control-Allow-Headers" -> "content-type"
    )
    Result(
      ResponseHeader(204, returnHeaders),
      HttpEntity.NoEntity
    )
  }
  /**
    * Action that allows the frontend to test if the current session is valid
    * @return If the session is not valid, a 403 response
    *         If the session is valid, a 200 response with the currently logged in userid in a json object
    */
  def isLoggedIn = IsAuthenticated { uid=> { request=>
    val adminRoles = config.getStringList("ldap.admin-groups").map(_.asScala).getOrElse(List("Administrator"))
    val isAdmin = config.get[String]("ldap.ldapProtocol") match {
      case "none"=>true
      case _=>checkRole(uid, adminRoles)
    }

    Ok(Json.obj("status"->"ok","uid"->uid, "isAdmin"->isAdmin))
  }}

  /**
    * Action that allows the frontend to test if the user is an admin
    * @return If the user is not an admin, a 403 response. If the user is an admin, a 200 response
    */
  def checkIsAdmin = IsAdmin {uid=> {request=>
    Ok(Json.obj("status"->"ok"))
  }}

  /**
    * Action to log out, by clearing the client's session cookie.
    * @return
    */
  def logout = Action { request=>
    Ok(Json.obj("status"->"ok","detail"->"Logged out")).withNewSession
  }

  /**
    * test raise an exception
    */
  def testexception = Action { request=>
    throw new RuntimeException("This is a test exception")
  }

  /**
    * test raise an exception that is caught and logged
    */
  def testcaughtexception = Action { request=>
    try{
      throw new RuntimeException("This is a test exception that was caught")
    } catch {
      case e:Throwable=>
        logger.error("Testcaughtexception", e)
        Ok(Json.obj("status"->"ok","detail"->"test exception was caught"))
    }
  }

  def getPublicDsn = Action { request=>
    try {
      val prop = new Properties()
      prop.load(getClass.getClassLoader.getResourceAsStream("sentry.properties"))
      val dsnString = prop.getProperty("public-dsn")
      if(dsnString==null)
        NotFound(Json.obj("status"->"error","detail"->"property public-dsn was not set"))
      else
        Ok(Json.obj("status"->"ok","publicDsn"->dsnString))
    } catch {
      case e:Throwable=>
        logger.error("Could not get publicDsn property: ", e)
        InternalServerError(Json.obj("status"->"error","detail"->e.toString))
    }
  }

  def makeHealthcheckBody(statusString:String, ldapCheck: Try[Unit], dbCheck:Try[Unit]) = {
    val ldapString = ldapCheck match {
      case Success(x)=>"ok"
      case Failure(err)=>err.toString
    }
    val dbString = dbCheck match {
      case Success(x)=>"ok"
      case Failure(err)=>err.toString
    }

    Json.obj(
      "status"->statusString,
      "ldap"->ldapString,
      "database"->dbString
    )
  }

  def healthcheck = Action.async { request=>
    val checkFutures = Future.sequence(Seq(LDAP.hasConnectionPool,dbHelper.healthcheck))

    checkFutures.map(resultSeq=>{
      val ldapCheck = resultSeq.head
      val dbCheck = resultSeq(1)

      if(ldapCheck.isFailure || dbCheck.isFailure){
        if(ldapCheck.isFailure) logger.error(s"LDAP Healthcheck is failing: ${ldapCheck.failed.get.toString}")
        if(dbCheck.isFailure) logger.error(s"DB Healthcheck is failing: ${dbCheck.failed.get.toString}")
        InternalServerError(makeHealthcheckBody("error",ldapCheck,dbCheck))
      } else{
        Ok(makeHealthcheckBody("ok",ldapCheck,dbCheck))
      }
    })
  }
}