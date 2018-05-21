package helpers

import auth.HMAC
import org.junit.runner.RunWith
import org.specs2.mutable._
import org.specs2.runner.JUnitRunner
import play.api.mvc.Headers
import play.api.test.FakeRequest

@RunWith(classOf[JUnitRunner])
class HmacAuth extends Specification {
  "HMAC.generateHmac" should {
    "generate a valid HMAC given a shared secret and string to sign" in {
      //println(HMAC.getAlgos.map(info=>s"${info._1}: ${info._2} (${info._3})").mkString("\n"))
      val result = HMAC.generateHMAC("thisIsMySharedSecret","someStringToSign")

      result mustEqual "VNAFDMVr8uIexQYW0jyMc4lC+8lXoPijg6eYfUqMB03jJsgX1QV/XfeqapI0n9s+"
    }
  }

  "HMAC.calculateHmac" should {
    "generate a valid HMAC given the correct request headers" in {
      val headers = Headers(("Date", "Tue, 15 Nov 1994 12:45:26 GMT"),
        ("X-Sha384-Checksum", "d1dc31c2a7828faf4477fa29faae132cc9663812fcd7ec6659f38da591216b113a668062ed73bdfd2f361b0d3979f1fe"),
        ("Content-Length", "123456")
      )
      val req = FakeRequest("GET","/path/to/endpoint").withHeaders(headers)
      val result = HMAC.calculateHmac(req,"thisIsMySharedSecret")

      result must beSome("GD5qXnVJqV+bsXn2YAJC+wCz72zvH6qS6xh1SSUpPnshShijxkFLc/8QZbHDf83E")
    }

    "return None if any headers are missing" in {
      val headers = Headers(("Date", "Tue, 15 Nov 1994 12:45:26 GMT"),
        ("Content-Length", "123456")
      )
      val req = FakeRequest("GET","/path/to/endpoint").withHeaders(headers)
      val result = HMAC.calculateHmac(req,"thisIsMySharedSecret")

      result must beNone
    }

    "return None if no headers are present" in {
      val req = FakeRequest("GET","/path/to/endpoint")
      val result = HMAC.calculateHmac(req,"thisIsMySharedSecret")

      result must beNone
    }
  }
}
