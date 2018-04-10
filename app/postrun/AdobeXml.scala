package postrun
import java.io._
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.Elem

trait AdobeXml {
  def getGzippedInputStream(fileName: String):Try[GZIPInputStream] = {
    try {
      val rawInputStream = new FileInputStream(fileName)
      Success(new GZIPInputStream(rawInputStream))
    } catch {
      case x:Throwable=>Failure(x)
    }
  }

  def getXmlFromGzippedFile(fileName:String):Future[Try[Elem]] = {
    getGzippedInputStream(fileName) match {
      case Failure(err)=>Future(Failure(err))
      case Success(inputStream)=>Future {
        Success(scala.xml.XML.load(inputStream))
      }.recover({
        case x:Throwable=>Failure(x)
      })
    }
  }

  def getGzippedOutputStream(fileName: String):Try[GZIPOutputStream] = try {
    val rawOutputStream = new FileOutputStream(fileName)
    Success(new GZIPOutputStream(rawOutputStream))
  } catch {
    case x:Throwable=>Failure(x)
  }

  def putXmlToGzippedFile(fileName:String, xmlData:Elem):Future[Try[Unit]] = {
    getGzippedOutputStream(fileName) match {
      case Failure(err)=>Future(Failure(err))
      case Success(outputStream)=>Future {
        val writer = new OutputStreamWriter(outputStream)
        scala.xml.XML.write(writer,xmlData,"UTF-8",true,null)
        writer.flush()
        Success(outputStream.finish())
      }.recoverWith({
        case x:Throwable=>Future(Failure(x))
      })
    }
  }
}
