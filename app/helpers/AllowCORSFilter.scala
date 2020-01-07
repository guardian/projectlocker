package helpers

import akka.stream.Materializer
import javax.inject.Inject
import org.slf4j.LoggerFactory
import play.api.Configuration
import play.api.mvc.{Filter, RequestHeader, Result}

import scala.concurrent.{ExecutionContext, Future}

object AllowCORSFunctions {
  private val logger = LoggerFactory.getLogger(getClass)
  def checkCorsOrigins(config:Configuration, request:RequestHeader):Either[String,String] = {
    if(!request.headers.hasHeader("Origin")) return Left("No origin header present, CORS not required")

    logger.debug(s"checkCorsOrigins: current origin is ${request.headers.get("Origin")}")
    if(!request.headers.hasHeader("Origin")) Left("CORS not applicable")
    config.getOptional[Seq[String]]("external.allowedFrontendDomains") match {
      case Some(allowedDomainsList)=>
        logger.debug(s"checkCorsOrigins: allowed urls are $allowedDomainsList")
        if(allowedDomainsList.contains(request.headers("Origin"))) Right(request.headers("Origin")) else Left(s"${request.headers("Origin")} is not an allowed domain")
      case None=>Left("No allowed origins configured")
    }
  }
}

class AllowCORSFilter @Inject() (config:Configuration)(implicit val mat:Materializer, ec:ExecutionContext) extends Filter {
  private final val logger = LoggerFactory.getLogger(getClass)

  logger.info(s"Starting up AllowCORSFilter")

  override def apply(next: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {
    logger.debug(s"AllowCORSFilter: $rh")
    AllowCORSFunctions.checkCorsOrigins(config, rh) match {
      case Right(allowedUrl)=>
        next(rh).map(_.withHeaders(
          "Access-Control-Allow-Origin"->allowedUrl,
          "Access-Control-Allow-Credentials"->"true"
        ))
      case Left(_)=>next(rh)
    }
  }
}
