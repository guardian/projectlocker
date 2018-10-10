package services
import java.security.MessageDigest
import java.io.{File, FileInputStream}
import org.apache.commons.io.FilenameUtils

import play.api.Logger

class CachebusterImpl extends Cachebuster {
  private val logger = Logger(getClass)
  val bufferSize = 10*1024
  protected val checkFiles = Seq("public/javascripts/bundle.js","public/stylesheets/overrides.css")

  protected val checksums:Map[String,String] = (for {
    filepath <- checkFiles
    maybeTuple <- getChecksum(filepath)
  } yield maybeTuple).toMap

  override def checksumFor(key: String): Option[String] = checksums.get(key)

  /**
    * recursively reads chunks from the file input stream and pushes them into the message digest.
    * this won't work for very very large files, as the memory is only freed once the entire file is processed,
    * but will be ok for the things we want to cache-bust on.
    * @param stream FileInputStream to read
    * @param pos current position in stream (start at 0)
    * @param d MessageDigest object to update
    * @return Updated MessageDigest
    */
  private def addNextChunk(stream:FileInputStream, pos:Int, d:MessageDigest):MessageDigest = {
    if(stream.available()>0) {
      val chunkSize = if (stream.available() > bufferSize) bufferSize else stream.available()

      val chunk = new Array[Byte](chunkSize)
      stream.read(chunk, 0, chunkSize)
      d.update(chunk)
      addNextChunk(stream, pos+chunkSize, d)
    } else {
      d
    }
  }

  /**
    * checksum the given file and return a tuple of (basename, checksum) or None
    * @param file filepath to check
    * @return
    */
  protected def getChecksum(file:String):Option[(String,String)] = {
    logger.debug(s"cachebuster is checking file $file")
    val d = MessageDigest.getInstance("MD5")

    val stream = try {
      new FileInputStream(new File(file))
    } catch {
      case e:Throwable=>
        logger.error(s"Could not open $file for cachebusting: ",e)
        return None
    }

    try{
      logger.debug(s"cachebuster reading in from $file")
      val finalDigest = addNextChunk(stream,0, d)
      stream.close()
      val finalChecksum=javax.xml.bind.DatatypeConverter.printHexBinary(finalDigest.digest())
      logger.debug(s"cachebuster got $finalChecksum for $file")
      Some((FilenameUtils.getName(file), finalChecksum))
    } catch {
      case e:Throwable=>
        stream.close()
        logger.error(s"Could not digest file $file", e)
        None
    }
  }
}
