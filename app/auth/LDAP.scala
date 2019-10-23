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
 * Modified by Andy Gallagher to improve error reporting when configuration is invalid
 */

package auth

import com.unboundid.ldap.sdk._
import com.unboundid.util.ssl.{SSLUtil, TrustAllTrustManager, TrustStoreTrustManager}
import java.security.MessageDigest

import Conf._
import play.api.Logger
import play.api.cache.SyncCacheApi

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

import scala.concurrent.ExecutionContext.Implicits.global

object LDAP {
  private val logger = Logger(getClass)
  
  protected val trustManager = {
    (ldapProtocol,ldapUseKeystore) match {
      case ("ldaps",true) =>
        Try { new TrustStoreTrustManager(trustStore,trustStorePass,trustStoreType,true) }
      case ("ldaps",false) =>
        Try { new TrustAllTrustManager() }
      case _ =>
        Success(null)// don't need a trust store
    }
  }

  // Initialize Multi-Server LDAP Connection Pool
  val connectionPool:Try[LDAPConnectionPool] = ldapProtocol match {
    case "ldaps" =>
      Try { new LDAPConnectionPool(new FailoverServerSet(serverAddresses, serverPorts,new SSLUtil(trustManager.get).createSSLSocketFactory()),new SimpleBindRequest(bindDN, bindPass), poolSize) }
    case "ldap" =>
      Try { new LDAPConnectionPool(new FailoverServerSet(serverAddresses, serverPorts),new SimpleBindRequest(bindDN, bindPass), poolSize) }
    case _ =>
      Failure(new RuntimeException(s"Invalid ldap protocol in settings: $ldapProtocol"))
  }

  def getDN (searchEntries: java.util.List[com.unboundid.ldap.sdk.SearchResultEntry]) : Option[String] = {
    searchEntries.size match {
      case 0 => None
      case _ => Some(searchEntries.get(0).getDN)
    }
  }

  def getUserDN (uid:String)(implicit cache:SyncCacheApi) : Option[String] = {
    if(connectionPool.isFailure) return None
    val cacheKey = "userDN." + uid
    val userDN: Option[String] = cache.getOrElseUpdate[Option[String]](cacheKey) {
      logger.debug("LDAP: get DN for " + uid)
      // Get DN for a given uid
      val searchEntries : java.util.List[com.unboundid.ldap.sdk.SearchResultEntry] = connectionPool.get
        .search(new SearchRequest(
          userBaseDN, 
          SearchScope.SUB,
          Filter.createEqualityFilter(uidAttribute,uid))
        )
        .getSearchEntries
      getDN(searchEntries)
    }
    userDN
  }

  /**
    * Returns false if the connection pool has either failed or there are no available connections
     * @return
    */
  def hasConnectionPool:Future[Try[Unit]] = Future {
    connectionPool
      .map(_.getCurrentAvailableConnections>0)
      .getOrElse(false) match {
      case true=>Success(())
      case false=>Failure(new RuntimeException("No LDAP pool connections available"))
    }
  }

  def getUserRoles (uid: String)(implicit cache:SyncCacheApi) : Option[List[String]] = {
    if(connectionPool.isFailure) return None
    val cacheKey = "userRoles." + uid
    logger.debug(s"cacheKey: $cacheKey")
    logger.debug(s"cache: $cache")
    val userRoles : Option[List[String]] = cache.getOrElseUpdate[Option[List[String]]](cacheKey,Duration.create(ldapCacheDuration,"seconds")) {
      logger.debug("LDAP: get roles for " + uid)
      try {
        val searchEntries : java.util.List[com.unboundid.ldap.sdk.SearchResultEntry] = connectionPool.get
          .search(new SearchRequest(
            userBaseDN,
            SearchScope.SUB,
            Filter.createEqualityFilter(uidAttribute,uid),roleMemberAttribute)
          )
          .getSearchEntries
        val groups : List[String] = searchEntries.get(0)
          .getAttributeValues("memberOf")
          .toList
          .map { _.split(",")(0).split("=")(1) }
        logger.debug(s"Got roles $groups")
        Some(groups)
      } catch {
        case ex:java.lang.IndexOutOfBoundsException=>
          logger.error(s"User $uid has no roles attached?", ex)
          None
        case lde: LDAPException =>
          logger.error("Could not look up ldap groups", lde)
          None
      }
    }
    logger.debug(s"Got user roles $userRoles")
    userRoles
  }

  def getRoleDN (role:String)(implicit cache:SyncCacheApi) : Option[String] = {
    if(connectionPool.isFailure) return None
    val cacheKey = "roleDN." + role
    val roleDN : Option[String] = cache.getOrElseUpdate[Option[String]](cacheKey) {
      logger.debug("LDAP: get DN for " + role)
      // Get DN for a given role
      val searchEntries : java.util.List[com.unboundid.ldap.sdk.SearchResultEntry] = connectionPool.get
        .search(new SearchRequest(
          roleBaseDN, 
          SearchScope.SUB,
          Filter.createEqualityFilter(roleAttribute,role))
        )
        .getSearchEntries
      getDN(searchEntries)
    }
    roleDN
  }

  def compareMember (roleDN: String, userDN: String) : Int = {
    if(connectionPool.isFailure) return -1
    logger.debug("LDAP: compare " + roleDN + " " + userDN)
    connectionPool.get
      .compare(new CompareRequest(roleDN,memberAttribute,userDN))
      .getResultCode
      .intValue
  }

  def isMember (role:String, uid:String)(implicit cache:SyncCacheApi, connectionPool:LDAPConnectionPool) : Int = {
    // Check if a given uid is a member of specified role

    val roleDN : Option[String] = getRoleDN(role)
    val userDN : Option[String] = getUserDN(uid)

    (roleDN,userDN) match {
      case (Some(r),Some(u)) =>
        compareMember(r,u)
      case (None,Some(u)) =>
        201 // role not found
      case (Some(r),None) =>
        202 // uid not found
      case (None,None) => 
        203 // role and uid not found
    }
  }

  def bind (uid:String,pass:String)(implicit cache:SyncCacheApi) : Try[Int] = Try {
    if(connectionPool.isFailure) return Failure(connectionPool.failed.get)

    val msg : String = uid + pass + "ela_salt_201406"
    val hash : String =  MessageDigest.getInstance("SHA-256")
      .digest(msg.getBytes)
      .foldLeft("")(
        (s: String, b: Byte) => 
          s + Character.forDigit((b & 0xf0) >> 4, 16) + Character.forDigit(b & 0x0f, 16)
      )
    val cacheKey = "bindResult." + hash
    val bindResult : Int = cache.getOrElseUpdate[Int](cacheKey,Duration(ldapCacheDuration,"seconds")) {
      getUserDN(uid) match {
        case Some(dn) =>
          logger.debug("LDAP: binding " + uid + " hash=" + hash)
          connectionPool.get
            .bindAndRevertAuthentication(new SimpleBindRequest(dn,pass))
            .getResultCode
            .intValue()
        case _ => 1
      }
    }
    bindResult
  }

  def getFullName (uid:String)(implicit cache:SyncCacheApi) : String = {
    if(connectionPool.isFailure) return ""
    val cacheKey = "userFullName." + uid
    val userFullName : String = cache.getOrElseUpdate[String](cacheKey,Duration(ldapCacheDuration,"seconds")) {
      logger.debug("LDAP: search " + uid + " Full Name")
      connectionPool.get
        .search(
          new SearchRequest(
            userBaseDN,
            SearchScope.SUB,
            Filter.createEqualityFilter(uidAttribute,uid),
            "name"
          )
        )
        .getSearchEntries
        .get(0)
        .getAttributeValue("name")
    }
    userFullName
  }

}
