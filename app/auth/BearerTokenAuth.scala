package auth

import java.time.Instant
import java.util.Date

import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import javax.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import play.api.mvc.RequestHeader
import play.api.Configuration
import play.api.libs.typedmap.TypedKey
import scala.util.{Failure, Success, Try}

object BearerTokenAuth {
  final val ClaimsAttributeKey = TypedKey[JWTClaimsSet]("claims")
}

/**
 * this class implements bearer token authentication. It's injectable because it needs to access app config.
 * You don't need to integrate it directly in your controller, it is required by the Security trait.
 *
 * This expects there to be an `auth` section in the application.conf which should contain two keys:
 * auth {
 *   adminClaim = "claim-field-indicating-admin"
 *   tokenSigningCert = """----- BEGIN CERTIFICATE -----
 *   {your certificate....}
 *   ----- END CERTIFICATE -----"""
 * }
 *
 * A given bearer token must authenticate against the provided certificate to be allowed access, and its expiry time
 * must not be in the past. The token's subject field ("sub") is used as the username.
 * Admin access is only granted if the token's field given by auth.adminClaim is a string that equates to "true" or "yes".
 *
 * So, in order to use it:
 *
 * class MyController @Inject() (controllerComponents:ControllerComponents, override bearerTokenAuth:BearerTokenAuth) extends AbstractController(controllerComponets) with Security { }
 * @param config application configuration object. This is normally provided by the injector
 */
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

  /**
   * returns the configured name for the claim field that will give whether a user is an admin or not.
   * It's included here because the Security trait is a mixin and can't access the config directly.
   * @return
   */
  def isAdminClaimName():String = {
    config.get[String]("auth.adminClaim")
  }

  /**
   * extracts the authorization token from the provided header
   * @param fromString complete Authorization header text
   * @return None if the header text does not match the expected format. The raw bearer token if it does.
   */
  def extractAuthorization(fromString:String):Option[String] =
    fromString match {
      case authXtractor(token)=>
        logger.debug("found valid base64 bearer")
        Some(token)
      case _=>
        logger.warn("no bearer token found or it failed to validate")
        None
    }

  /**
   * loads in the public certificate used for validating the bearer tokens from configuration
   * @return either the passed JWK object or a Failure indicating why it would not load.
   */
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

  /**
   * check the given parsed claims set to see if the token has already expired
   * @param claims JWTClaimsSet representing the token under consideration
   * @return a Try, containing either the claims set or a failure indicating the reason authentication failed. This is
   *         to make composition easier.
   */
  def checkExpiry(claims:JWTClaimsSet) = {
    if(claims.getExpirationTime.before(Date.from(Instant.now()))) {
      logger.debug(s"JWT was valid but expired at ${claims.getExpirationTime.formatted("YYYY-MM-dd HH:mm:ss")}")
      Failure(new RuntimeException("Token has expired"))
    } else {
      Success(claims)
    }
  }

  /**
    * performs the JWT authentication against a given header.
   * This should not be called directly, but is done in the Security trait as part of IsAuthenticated or IsAdmin.
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
