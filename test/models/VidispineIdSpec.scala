package models

import org.junit.runner._
import org.specs2.mutable.Specification
import org.specs2.runner._

@RunWith(classOf[JUnitRunner])
class VidispineIdSpec extends Specification {
  "VidispineId.fromString" should {
    "initialise from a valid id string" in {
      val result = VidispineId.fromString("VX-1234")
      result must beSuccessfulTry(VidispineId("VX",1234))
    }

    "return a failure if the string does not parse" in {
      val result = VidispineId.fromString("invalid")
      result must beFailedTry
    }
  }

  "VidispineId.toString" should {
    "return just a recombined id" in {
      val result = VidispineId("VX",4567).toString
      result mustEqual "VX-4567"
    }
  }
}
