import helpers.JythonRunner
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

@RunWith(classOf[JUnitRunner])
class JythonRunnerSpec extends Specification {
  sequential

  "JythonRunner.runScript" should {
    "run an external script" in {
      val result = JythonRunner.runScript("postrun/test_scripts/basic_test_1.py")
      result.raisedError must beNone
      result.stdOutContents mustEqual
        """Hello world!
          |""".stripMargin
      result.stdErrContents mustEqual ""
    }

    "handle exceptions" in {
      val result = JythonRunner.runScript("postrun/test_scripts/error_test_2.py")
      result.raisedError must beSome
      result.stdOutContents mustEqual ""
      result.stdErrContents mustEqual ""
      result.raisedError.get.toString mustEqual
              """Traceback (most recent call last):
                |  File "postrun/test_scripts/error_test_2.py", line 1, in <module>
                |    raise StandardError("My hovercraft is full of eels")
                |StandardError: My hovercraft is full of eels
                |""".stripMargin
    }

    "be able to load a script with external dependencies" in {
      val result = JythonRunner.runScript("postrun/test_scripts/import_test_3.py")
      result.raisedError must beNone
      result.stdOutContents mustEqual
        """Hello world!
          |""".stripMargin
      result.stdErrContents mustEqual ""
    }

    "call a specific function with arguments" in {
      val args = Map("project_id"->"AA-1234","something_else"->"rabbit rabbit")

      val result = JythonRunner.runScript("postrun/test_scripts/args_test_4.py", args)
      result must beSuccessfulTry
      result.get.raisedError must beNone
      result.get.stdOutContents mustEqual
        """I was provided with {'something_else': 'rabbit rabbit', 'project_id': 'AA-1234'}
          |""".stripMargin
      result.get.stdErrContents mustEqual ""
    }
  }
}
