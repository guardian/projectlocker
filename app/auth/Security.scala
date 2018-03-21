/*
 * Copyright (C) 2015 Jason Mar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modified by Andy Gallagher to provide extra IsAuthenticated implementations for async actions etc.
 */

package auth

import play.api.mvc._
import play.api.libs.Files.TemporaryFile
import scala.collection.JavaConverters._
import play.api.{ConfigLoader, Configuration, Logger}
import play.api.cache.SyncCacheApi
import play.api.libs.json._

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

trait Security {
  implicit val cache:SyncCacheApi
  val logger: Logger = Logger(this.getClass)

  /**
    * look up an ldap user in the session.
    * @param request HTTP request object
    * @return Option containing uid if present or None
    */
  private def ldapUsername(request: RequestHeader) = Conf.ldapProtocol match {
    case "none"=>Some("noldap")
    case _=>request.session.get("uid")
  }

  /**
    * look up an hmac user
    * @param header HTTP request object
    * @param auth Authorization token as passed from the client
    */
  private def hmacUsername(header: RequestHeader, auth: String):Option[String] = {
    val authparts = auth.split(":")

    logger.debug(s"authparts: ${authparts.mkString(":")}")
    logger.debug(s"headers: ${header.headers.toSimpleMap.toString}")
    if(Conf.sharedSecret.isEmpty){
      logger.error("Unable to process server->server request, shared_secret is not set in application.conf")
      return None
    }

    HMAC.calculateHmac(header, Conf.sharedSecret).flatMap(calculatedSig=>{if(calculatedSig==authparts(1)) Some(authparts(0)) else None})
  }

  //if this returns something, then we are logged in
  private def username(request:RequestHeader) = request.headers.get("Authorization") match {
    case Some(auth)=>
      logger.debug("got Auth header, doing hmac auth")
      hmacUsername(request,auth)
    case None=>
      logger.debug("no Auth header, doing session auth")
      ldapUsername(request)
  }

  private def onUnauthorized(request: RequestHeader) = {
    Results.Forbidden(Json.obj("status"->"error","detail"->"Not logged in"))
  }

  def IsAuthenticated(f: => String => Request[AnyContent] => Result) = Security.Authenticated(username, onUnauthorized) {
    uid => Action(request => f(uid)(request))
  }

  def IsAuthenticatedAsync(f: => String => Request[AnyContent] => Future[Result]) = Security.Authenticated(username, onUnauthorized) {
    uid => Action.async(request => f(uid)(request))
  }

  def IsAuthenticatedAsync[A](b: BodyParser[A])(f: => String => Request[A] => Future[Result]) = Security.Authenticated(username, onUnauthorized) {
    uid=> Action.async(b)(request => f(uid)(request))
  }

  def IsAuthenticated(b: BodyParser[MultipartFormData[TemporaryFile]] = BodyParsers.parse.multipartFormData)(f: => String => Request[MultipartFormData[TemporaryFile]] => Result) = {
    Security.Authenticated(username, onUnauthorized) { uid => Action(b)(request => f(uid)(request)) }
  }

  def HasRole(requiredRoles: List[String])(f: => String => Request[AnyContent] => Result) = IsAuthenticated {
    uid => 
      request => 
        LDAP.getUserRoles(uid) match {
          case Some(userRoles) if requiredRoles.intersect(userRoles).nonEmpty => f(uid)(request)
          case _ =>
            if(sys.env.contains("CI"))  //allow admin functions when under test
              f(uid)(request)
            else
              Results.Forbidden
        }
  }

  def HasRoleAsync(requiredRoles: List[String])(f: => String => Request[AnyContent] => Future[Result]) = IsAuthenticatedAsync {
    uid =>
      request =>
        LDAP.getUserRoles(uid) match {
          case Some(userRoles) if requiredRoles.intersect(userRoles).nonEmpty => f(uid)(request)
          case _ =>
            if(sys.env.contains("CI"))  //allow admin functions when under test
              f(uid)(request)
            else
              Future(Results.Forbidden)
        }
  }

  def HasRoleAsync[A](requiredRoles: List[String])(b: BodyParser[A])(f: => String => Request[A] => Future[Result]) = IsAuthenticatedAsync[A](b) {
    uid =>
      request =>
        LDAP.getUserRoles(uid) match {
          case Some(userRoles) if requiredRoles.intersect(userRoles).nonEmpty => f(uid)(request)
          case _ =>
            if(sys.env.contains("CI"))  //allow admin functions when under test
              f(uid)(request)
            else
              Future(Results.Forbidden)
        }
  }

  def IsAdmin(f: => String => Request[AnyContent] => Result) = HasRole(Conf.adminGroups.asScala.toList)(f)

  def IsAdminAsync[A](b: BodyParser[A])(f: => String => Request[A] => Future[Result]) = HasRoleAsync[A](Conf.adminGroups.asScala.toList)(b)(f)

  def IsAdminAsync(f: => String => Request[AnyContent] => Future[Result]) = HasRoleAsync[AnyContent](Conf.adminGroups.asScala.toList)(BodyParsers.parse.anyContent)(f)

  def HasRoleUpload(requiredRoles: List[String])(b: BodyParser[MultipartFormData[TemporaryFile]] = BodyParsers.parse.multipartFormData)(f: => String => Request[MultipartFormData[TemporaryFile]] => Result) = IsAuthenticated(b) {
    uid => 
      request => 
        LDAP.getUserRoles(uid) match {
          case Some(userRoles) if requiredRoles.intersect(userRoles).nonEmpty => f(uid)(request)
          case _ => 
            Results.Forbidden
        }
  }

  def GetRole(uid: String) : Option[List[String]] = {
    uid match {
      case "sv-ela-t" => Some(List("admin","user"))
      case _ => None
    }
  }

}
