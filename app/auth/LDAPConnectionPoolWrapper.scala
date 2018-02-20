package auth

import javax.inject.Singleton

import auth.Conf._
import com.google.inject.ImplementedBy
import com.unboundid.ldap.sdk.{FailoverServerSet, LDAPConnectionPool, SimpleBindRequest}
import com.unboundid.util.ssl.{SSLUtil, TrustAllTrustManager, TrustStoreTrustManager}

import scala.util.{Failure, Success, Try}

@ImplementedBy(classOf[LDAPConnectionPoolWrapperImpl])
trait LDAPConnectionPoolWrapper {
  def connectionPool:Try[LDAPConnectionPool]
}

@Singleton
class LDAPConnectionPoolWrapperImpl extends LDAPConnectionPoolWrapper {
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
  private val ldapConnectionPool = ldapProtocol match {
    case "ldaps" =>
      Try { new LDAPConnectionPool(new FailoverServerSet(serverAddresses, serverPorts,new SSLUtil(trustManager.get).createSSLSocketFactory()),new SimpleBindRequest(bindDN, bindPass), poolSize) }
    case "ldap" =>
      Try { new LDAPConnectionPool(new FailoverServerSet(serverAddresses, serverPorts),new SimpleBindRequest(bindDN, bindPass), poolSize) }
    case _ =>
      Failure(new RuntimeException(s"Invalid ldap protocol in settings: $ldapProtocol"))
  }

  def connectionPool : Try[LDAPConnectionPool] = ldapConnectionPool
}

