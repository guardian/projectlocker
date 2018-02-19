package helpers

import java.io.ByteArrayOutputStream

import org.python.util.PythonInterpreter

import scala.util.{Failure, Success, Try}

case class JythonOutput(stdOutContents: String, stdErrContents: String, raisedError: Option[Throwable])

class JythonRunner {

  import org.python.util.PythonInterpreter
  import java.util.Properties

  val props = new Properties
  //props.put("python.home", "path to the Lib folder")
  props.put("python.console.encoding", "UTF-8") // Used to prevent: console: Failed to install '': java.nio.charset.UnsupportedCharsetException: cp0.
  //props.put("python.security.respectJavaAccessibility", "false") //don't respect java accessibility, so that we can access protected members on subclasses
  props.put("python.import.site", "false")

  val preprops: Properties = System.getProperties

  PythonInterpreter.initialize(preprops, props, new Array[String](0))


  def runScript(scriptName: String) = {
    val outStream = new ByteArrayOutputStream
    val errStream = new ByteArrayOutputStream

    val interpreter = new PythonInterpreter()

    interpreter.setOut(outStream)
    interpreter.setErr(errStream)
    val result = Try {
      interpreter.execfile(scriptName)
    }

    val raisedError = result match {
      case Success(nothing) => None
      case Failure(error) => Some(error)
    }

    JythonOutput(outStream.toString, errStream.toString, raisedError)
  }
}
