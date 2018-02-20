package helpers

import java.io.ByteArrayOutputStream

import org.python.core.{PyDictionary, PyObject, PyString}
import org.python.util.PythonInterpreter
import scala.collection.JavaConverters._

import scala.util.{Failure, Success, Try}

/**
  * this case class represents the result of the invokation of a script in Jython
  * @param stdOutContents what the script put to stdout
  * @param stdErrContents what the script put to stderr
  * @param raisedError either None, if the run completed successfully, or a Throwable representing an error that occurred
  */
case class JythonOutput(stdOutContents: String, stdErrContents: String, raisedError: Option[Throwable])

object JythonRunner {

  import org.python.util.PythonInterpreter
  import java.util.Properties

  val props = new Properties
  props.put("python.home", "postrun/lib/python")
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

  /**
    * Runs the given script, with a string->string map of arguments.
    * This style of invokation requires  the Python script to define a function `postrun`, which will receive
    * the string->string map in the form of a dictionary of kwargs.
    * @param scriptName name of script to call
    * @param args string-string map
    * @return Try containing a [[JythonOutput]] if successful or a relevant error if not
    */
  def runScript(scriptName: String, args:Map[String,String]):Try[JythonOutput] = {
    val outStream = new ByteArrayOutputStream
    val errStream = new ByteArrayOutputStream

    val interpreter = new PythonInterpreter()

    interpreter.setOut(outStream)
    interpreter.setErr(errStream)

    //the cast is annoying but it should always work, since PyString is a subclass of PyObject. No idea why
    // func.__call__ seems to not like this though.
    val pythonifiedArgs = args.map(kvTuple=>new PyString(kvTuple._2).asInstanceOf[PyObject])
    val pythonifiedNames = args.keys

    try {
      interpreter.execfile(scriptName)
      val func = interpreter.get("postrun")

      val result = Try { func.__call__(pythonifiedArgs.toArray, pythonifiedNames.toArray) }
      val raisedError = result match {
        case Success(nothing) => None
        case Failure(error) => Some(error)
      }

      Success(JythonOutput(outStream.toString, errStream.toString, raisedError))
    } catch {
      case err:Throwable=>Failure(err)
    }
  }
}
