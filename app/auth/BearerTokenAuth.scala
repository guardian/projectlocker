package auth

import java.time.Instant
import java.util.Date

import akka.stream.Materializer
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import javax.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import play.api.mvc.{Filter, RequestHeader, ResponseHeader, Result, Results}
import play.api.{Configuration, Logging}
import play.api.libs.json.Json
import play.api.libs.typedmap.TypedKey

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

object BearerTokenAuth {
  final val ClaimsAttributeKey = TypedKey[JWTClaimsSet]("claims")
}

@Singleton
class BearerTokenAuth @Inject() (config:Configuration) {
  private val logger = LoggerFactory.getLogger(getClass)

  //see https://stackoverflow.com/questions/475074/regex-to-parse-or-validate-base64-data
  //it is not the best option but is the simplest that will work here
  private val authXtractor = "^Bearer\\s+([a-zA-Z0-9+/._-]*={0,3})$".r
  private val maybeVerifier = loadInKey() match {
    case Failure(err)=>
      if(!sys.env.contains("CI")) logger.warn(s"No token validation cert in config so bearer token auth will not work. Error was ${err.getMessage}")
      None
    case Success(jwk)=>
      Some(new RSASSAVerifier(jwk.toRSAKey))
  }

  def isAdminClaimName() = {
    config.get[String]("auth.adminClaim")
  }
  def extractAuthorization(fromString:String) =
    fromString match {
      case authXtractor(token)=>
        logger.debug("found valid base64 bearer")
        Some(token)
      case _=>
        logger.warn("no bearer token found or it failed to validate")
        None
    }

  //this fails if it can't load, deliberately; it is called at init so should block the server from
  //initialising. This is desired fail-fast behaviour
  def loadInKey() = Try {
    val pemCertData = config.get[String]("auth.tokenSigningCert")
    JWK.parseFromPEMEncodedX509Cert(pemCertData)
  }

  /**
    * try to validate the given token with the key provided
    * returns a JWTClaimsSet if successful
    * the Try is cast to a Future to make composition easier
    * @param token JWT token to verify
    * @return a Future, containing a JWTClaimsSet. The Future fails if it can't be verified
    */
  def validateToken(token:String) = {
    logger.debug(s"validating token $token")
    Try { SignedJWT.parse(token) }.flatMap(signedJWT=>
      maybeVerifier match {
        case Some(verifier) =>
          if (signedJWT.verify(verifier)) {
            logger.debug("verified JWT")
            Success(signedJWT.getJWTClaimsSet)
          } else {
            Failure(new RuntimeException("Failed to validate JWT"))
          }
        case None =>
          Failure(new RuntimeException("No signing cert configured"))
      })
  }

  def checkExpiry(claims:JWTClaimsSet) = {
    if(claims.getExpirationTime.before(Date.from(Instant.now()))) {
      logger.debug(s"JWT was valid but expired at ${claims.getExpirationTime.formatted("YYYY-MM-dd HH:mm:ss")}")
      Failure(new RuntimeException("Token has expired"))
    } else {
      Success(claims)
    }
  }

  /**
    * perform the JWT authentication
    * @param rh request header
    * @return a Try, containing the "username" parameter if successful or a Failure if not
    */
  def apply(rh: RequestHeader): Try[JWTClaimsSet] = {
    rh.headers.get("Authorization") match {
      case Some(authValue)=>
        extractAuthorization(authValue)
          .map(validateToken)
          .getOrElse(Failure(new RuntimeException("No authorization was present")))
          .flatMap(checkExpiry)
          .map(claims=>{
            rh.addAttr(BearerTokenAuth.ClaimsAttributeKey, claims)
            claims
          })
      case None=>
        logger.error("Attempt to access without authorization")
        Failure(new RuntimeException("Attempt to access without authorization"))
    }
  }
}
