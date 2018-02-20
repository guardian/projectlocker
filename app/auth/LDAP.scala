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
 */

package auth

import com.unboundid.ldap.sdk._
import com.unboundid.util.ssl.{SSLUtil, TrustAllTrustManager, TrustStoreTrustManager}
import java.security.MessageDigest

import Conf._
import play.api.cache.SyncCacheApi

import scala.concurrent.duration._
import scala.util.Try

object LDAP {
  def getDN (searchEntries: java.util.List[com.unboundid.ldap.sdk.SearchResultEntry]) : Option[String] = {
    searchEntries.size match {
      case 0 => None
      case _ => Some(searchEntries.get(0).getDN)
    }
  }

  def getUserDN (uid:String)(implicit cache:SyncCacheApi, connectionPool:LDAPConnectionPool) : Option[String] = {
    if(connectionPool==null) return None
    val cacheKey = "userDN." + uid
    val userDN: Option[String] = cache.getOrElseUpdate[Option[String]](cacheKey) {
      println("LDAP: get DN for " + uid)
      // Get DN for a given uid
      val searchEntries : java.util.List[com.unboundid.ldap.sdk.SearchResultEntry] = connectionPool
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
  
  def getUserRoles (uid: String)(implicit cache:SyncCacheApi, connectionPool:LDAPConnectionPool) : Option[List[String]] = {
    if(connectionPool==null) return None
    val cacheKey = "userRoles." + uid
    val userRoles : Option[List[String]] = cache.getOrElseUpdate[Option[List[String]]](cacheKey,Duration.create(ldapCacheDuration,"seconds")) {
      println("LDAP: get roles for " + uid)
      try {
        val searchEntries : java.util.List[com.unboundid.ldap.sdk.SearchResultEntry] = connectionPool
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
        Some(groups)
      } catch { 
        case lde: LDAPException => 
          None 
      }
    }
    userRoles
  }

  def getRoleDN (role:String)(implicit cache:SyncCacheApi, connectionPool:LDAPConnectionPool) : Option[String] = {
    if(connectionPool==null) return None
    val cacheKey = "roleDN." + role
    val roleDN : Option[String] = cache.getOrElseUpdate[Option[String]](cacheKey) {
      println("LDAP: get DN for " + role)
      // Get DN for a given role
      val searchEntries : java.util.List[com.unboundid.ldap.sdk.SearchResultEntry] = connectionPool
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

  def compareMember (roleDN: String, userDN: String)(implicit connectionPool:LDAPConnectionPool) : Int = {
    if(connectionPool==null) return -1
    println("LDAP: compare " + roleDN + " " + userDN)
    connectionPool
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

  def bind (uid:String,pass:String)(implicit cache:SyncCacheApi, connectionPool:LDAPConnectionPool) : Try[Int] = Try {
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
          println("LDAP: binding " + uid + " hash=" + hash)
          connectionPool
            .bindAndRevertAuthentication(new SimpleBindRequest(dn,pass))
            .getResultCode
            .intValue()
        case _ => 1
      }
    }
    bindResult
  }

  def getFullName (uid:String)(implicit cache:SyncCacheApi, connectionPool:LDAPConnectionPool) : String = {
    if(connectionPool==null) return ""
    val cacheKey = "userFullName." + uid
    val userFullName : String = cache.getOrElseUpdate[String](cacheKey,Duration(ldapCacheDuration,"seconds")) {
      println("LDAP: search " + uid + " Full Name")
      connectionPool
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
