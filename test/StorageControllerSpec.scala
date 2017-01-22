import javax.inject.Inject

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.test._
import play.api.inject.bind
import testHelpers.TestDatabase
import play.api.{Application, Logger}
import play.api.http.HttpEntity.Strict

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class StorageControllerSpec extends Specification {
  //needed for body.consumeData
  implicit val system = ActorSystem("storage-controller-spec")
  implicit val materializer = ActorMaterializer()

  val logger: Logger = Logger(this.getClass)

  //can over-ride bindings here. see https://www.playframework.com/documentation/2.5.x/ScalaTestingWithGuice
  val application:Application = new GuiceApplicationBuilder()
    .overrides(bind[DatabaseConfigProvider].to[TestDatabase.testDbProvider])
    .build

  "StorageController" should {

    "send 400 on a bad request" in TestDatabase.withTestDatabase { db=>
      val response = route(application,FakeRequest(GET, "/storage/boum")).get

      status(response) must equalTo(BAD_REQUEST)
    }

    "return valid data for a valid storage" in TestDatabase.withTestDatabase { db=>
      val response = route(application, FakeRequest(GET, "/storage/1")).get
      response.onComplete(maybeResult =>
        maybeResult.get.body.consumeData.onComplete(content=>
          logger.debug(content.getOrElse(ByteString("(no data)")).decodeString("UTF-8"))
        )
      )
      status(response) must equalTo(OK)

    }
  }
}
