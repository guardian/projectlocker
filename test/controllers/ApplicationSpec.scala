package controllers

import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import utils.MockedCacheApi

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */

@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends PlaySpecification with MockedCacheApi {
  sequential
  tag("controllers")

  "Application" should {
    "render the index page" in new WithApplication {
      val home = route(app, FakeRequest(GET, "/")).get

        status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "text/html")
      contentAsString(home) must contain ("<title>Projectlocker</title>")
    }
  }
}
