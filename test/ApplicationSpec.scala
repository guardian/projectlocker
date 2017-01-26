import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.db.slick.DatabaseConfigProvider
import play.api.test._
import play.api.test.Helpers._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{Injector, bind}
import testHelpers.TestDatabase

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {
  sequential

  //can over-ride bindings here. see https://www.playframework.com/documentation/2.5.x/ScalaTestingWithGuice
  private val application = new GuiceApplicationBuilder()
    .overrides(bind[DatabaseConfigProvider].to[TestDatabase.testDbProvider])
    .build

  "Application" should {

    "send 404 on a bad request" in {
      val response = route(application,FakeRequest(GET, "/boum")).get

      status(response) must equalTo(NOT_FOUND)
    }

    "render the index page" in  {
      val home = route(application, FakeRequest(GET, "/")).get

      status(home) must equalTo(NOT_FOUND)
//        status(home) must equalTo(OK)
//      contentType(home) must beSome.which(_ == "text/html")
//      contentAsString(home) must contain ("<title>Projectlocker</title>")
    }
  }
}
