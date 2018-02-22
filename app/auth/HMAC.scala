package auth

import play.api.Logger
import play.api.mvc.{AnyContent, Request, RequestHeader}
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.security._
import java.util.Base64

import collection.JavaConverters._

object HMAC {
  val logger = Logger("app.auth.HMAC")

  /**
    * generate the HMAC digest of a string, given a shared secret to encrypt it with
    * @param sharedSecret passphrase to encrypt with
    * @param preHashString content to digest
    * @return Base64 encoded string of the hmac digest
    */
  def generateHMAC(sharedSecret: String, preHashString: String): String = {
    val secret = new SecretKeySpec(sharedSecret.getBytes, "HmacSHA384")   //Crypto Funs : 'SHA256' , 'HmacSHA1'
    val mac = Mac.getInstance("HmacSHA384")
    mac.init(secret)
    val hashString: Array[Byte] = mac.doFinal(preHashString.getBytes)
    Base64.getEncoder.encodeToString(hashString) //new String(hashString.map(_.toChar))
  }

  /**
    * Take the relevant request headers and calculate what the digest should be
    * @param request Play request, must contain the headers Date, Content-Length, X-Sha384-Checksum
    * @param sharedSecret passphrase to encrypt with
    * @return Option containing the hmac digest, or None if any headers were missing
    */
  def calculateHmac(request: RequestHeader, sharedSecret: String):Option[String] = try {
    val string_to_sign = s"${request.headers.get("Date").get}\n${request.headers.get("Content-Length").getOrElse("0")}\n${request.headers.get("X-Sha384-Checksum").get}\n${request.method}\n${request.path}"
    Logger.debug(s"Incoming request, string to sign: $string_to_sign")
    val hmac = generateHMAC(sharedSecret, string_to_sign)
    Logger.debug(s"HMAC generated: $hmac")
    Some(hmac)
  } catch {
      case e:java.util.NoSuchElementException=>
        Logger.debug(e.toString)
        None
  }

  def validateContentChecksum(rq: Request[AnyContent]):Option[Boolean] = {
    if(rq.headers.get("X-Sha384-Checksum").isEmpty) return None //no point validating if there is no checksum to start with!

    val digester = MessageDigest.getInstance("SHA-384")
    val contentBuffer = rq.body.asRaw
    contentBuffer match {
      case Some(buffer)=>
        buffer.asBytes() match {
          case Some(bytes)=>
            val encodedDigest = Base64.getEncoder.encodeToString(digester.digest(bytes.toArray))
            Logger.debug(s"validateContentChecksum - got checksum $encodedDigest")
            if(encodedDigest==rq.headers.get("X-Sha384-Checksum").get) Some(true) else Some(false)
          case None=>
            Logger.error("checksum validation of upload buffered to disk is not yet implemented")
            None
        }
    }
  }
  /**
    * Returns information about the available crypto algorithms on this platform
    * @return
    */
  def getAlgos:Array[Tuple3[String,String,String]] = {
    for {
      provider <- Security.getProviders
      key <- provider.stringPropertyNames.asScala
    } yield Tuple3(provider.getName, key, provider.getProperty(key))

  }
}
