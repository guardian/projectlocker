package helpers

import java.io.{ByteArrayOutputStream, File}
import java.nio.file.{Path, Paths}
import java.util.Properties

import models.PostrunAction
import org.python.core.{PyObject, PyString}
import org.python.util.PythonInterpreter
import play.api.{Configuration, Logger}

import scala.util.{Failure, Success, Try}
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * this case class represents the result of the invokation of a script in Jython
  * @param stdOutContents what the script put to stdout
  * @param stdErrContents what the script put to stderr
  * @param newDataCache updated data cache object containing any key-values output by this script
  * @param raisedError either None, if the run completed successfully, or a Throwable representing an error that occurred
  */
case class JythonOutput(stdOutContents: String, stdErrContents: String, newDataCache:PostrunDataCache, raisedError: Option[Throwable])

object JythonRunner {
  private val logger = Logger(this.getClass)

  def initialise = {
    logger.info("Initialising jython runner")
    val props = new Properties
    props.put("python.home", "postrun/lib/python")
    props.put("python.path", "postrun/lib/python:postrun/lib:postrun/scripts:postrun")
    props.put("python.console.encoding", "UTF-8") // Used to prevent: console: Failed to install '': java.nio.charset.UnsupportedCharsetException: cp0.
    props.put("python.import.site", "false")

    val preprops: Properties = System.getProperties

    PythonInterpreter.initialize(preprops, props, new Array[String](0))
  }

  /**
    * runs all available postrun actions through the interpreter, to make sure that they are compiled for when the user
    * needs them
    * @param db implicitly provided database object
    * @return
    */
  def precompile(implicit db:slick.jdbc.PostgresProfile#Backend#Database, config:Configuration):Future[Seq[Try[String]]] = {
    val interpreter = new PythonInterpreter()

    PostrunAction.allEntries.map({
      case Success(entries)=>
        entries.map(entry=>{
          try {
            interpreter.execfile(entry.getScriptPath.toString)
            Success(entry.runnable)
          } catch {
            case e:Throwable=>
              Failure(e)
          }
        })
      case Failure(error)=>Seq(Failure(error))
    })
  }
}

class JythonRunner {
  import org.python.util.PythonInterpreter
  import java.util.Properties
  private val logger = Logger(this.getClass)
  private val interpreter = new PythonInterpreter()

  def getAbsolutePath(scriptName: String):String = new File(scriptName).getCanonicalPath

  def runScript(scriptName: String, dataCache: PostrunDataCache) = {
    val outStream = new ByteArrayOutputStream
    val errStream = new ByteArrayOutputStream

    interpreter.setOut(outStream)
    interpreter.setErr(errStream)
    interpreter.set("__file__", new PyString(getAbsolutePath(scriptName)))

    val result = Try {
      interpreter.execfile(scriptName)
    }

    val raisedError = result match {
      case Success(nothing) => None
      case Failure(error) => Some(error)
    }

    JythonOutput(outStream.toString, errStream.toString, dataCache, raisedError)
  }

  /**
    * convenience function to run the script and wait for result
    */
  def runScript(scriptName: String, args:Map[String,String], dataCache:PostrunDataCache)(implicit timeout:Duration):Try[JythonOutput] =
    Await.result(runScriptAsync(scriptName, args, dataCache), timeout)

  /**
    * Runs the given script, with a string->string map of arguments.
    * This style of invokation requires the Python script to define a function `postrun`, which will receive
    * the string->string map in the form of a dictionary of kwargs.
    * @param scriptName name of script to call
    * @param args string-string map of arguments passed as kwargs to the `postrun` function in the script
    * @param dataCache [[PostrunDataCache]] object representing information to pass to the script
    * @return Try containing a [[JythonOutput]] if successful or a relevant error if not
    */
  def runScriptAsync(scriptName: String, args:Map[String,String], dataCache:PostrunDataCache):Future[Try[JythonOutput]] = Future {
    val outStream = new ByteArrayOutputStream
    val errStream = new ByteArrayOutputStream

    interpreter.setOut(outStream)
    interpreter.setErr(errStream)
    interpreter.set("__file__", new PyString(getAbsolutePath(scriptName)))

    //the cast is annoying but it should always work, since PyString is a subclass of PyObject. No idea why
    // func.__call__ seems to not like this though.
    val pythonifiedArgs = args.map(kvTuple=>new PyString(kvTuple._2).asInstanceOf[PyObject]).toArray
    val pythonifiedNames = args.keys.toArray
    val updatedPythonifiedArgs = pythonifiedArgs ++ Array(dataCache.asPython.asInstanceOf[PyObject])
    val updatedPythonifiedNames = pythonifiedNames ++ Array("dataCache")

    logger.debug(s"updatedPythonifiedArgs = ${updatedPythonifiedArgs.toString}")
    logger.debug(s"updatedPythonifiedNames = ${updatedPythonifiedNames.toString}")

    try {

      interpreter.execfile(scriptName)
      val func = interpreter.get("postrun")

      val result = Try { func.__call__(updatedPythonifiedArgs, updatedPythonifiedNames) }
      result match {
        case Success(returnedObject) =>
          val updatedDataCache = dataCache ++ returnedObject
          logger.debug(s"Updated data cache: ${updatedDataCache.asPython.toString}")
          Success(JythonOutput(outStream.toString, errStream.toString, updatedDataCache, None))
        case Failure(error) =>
          Success(JythonOutput(outStream.toString, errStream.toString, dataCache, Some(error)))
      }


    } catch {
      case err:Throwable=>
        logger.error("Could not start postrun script: ", err)
        Failure(err)
    }
  }
}
