import helpers.JythonRunner
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

@RunWith(classOf[JUnitRunner])
class JythonRunnerSpec extends Specification {
  sequential

  "JythonRunner.runScript" should {
    "run an external script" in {
      val r = new JythonRunner()

      val result = r.runScript("conf/test_scripts/basic_test_1.py")
      result.raisedError must beNone
      result.stdOutContents mustEqual
        """Hello world!
          |""".stripMargin
      result.stdErrContents mustEqual ""
    }

    "handle exceptions" in {
      val r = new JythonRunner()

      val result = r.runScript("conf/test_scripts/error_test_2.py")
      result.raisedError must beSome
      result.stdOutContents mustEqual ""
      result.stdErrContents mustEqual ""
      result.raisedError.get.toString mustEqual
              """Traceback (most recent call last):
                |  File "conf/test_scripts/error_test_2.py", line 1, in <module>
                |    raise StandardError("My hovercraft is full of eels")
                |StandardError: My hovercraft is full of eels
                |""".stripMargin
    }
  }
}
