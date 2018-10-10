package services

import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.mutable._
import org.specs2.runner.JUnitRunner
import play.api.Logger

@RunWith(classOf[JUnitRunner])
class TestCachebusterImpl extends Specification with Mockito {
  "CachebusterImpl.getChecksum" should {
    "checksum an existing file and return both basename and checksum" in {
      val cs = new CachebusterImpl {
        def testGetChecksum(file:String) = getChecksum(file)
      }

      val result = cs.testGetChecksum("public/stylesheets/overrides.css")
      result must beSome(("overrides.css","408C50EAAC5C7B3DD2DB6D9410D4687F"))
    }

    "return None if the file does not exist" in {
      val mockLogger = mock[Logger]
      mockLogger.error(anyString)

      val cs = new CachebusterImpl {
        def testGetChecksum(file:String) = getChecksum(file)
      }

      val result = cs.testGetChecksum("fasfdfadslfadsm,nfads")
      result must beNone
    }
  }
}
