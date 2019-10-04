package drivers

import java.io.{EOFException, InputStream, OutputStream}
import java.nio.ByteBuffer
import java.nio.file.attribute.FileTime
import java.time.ZonedDateTime

import akka.stream.Materializer
import com.om.mxs.client.japi.{MatrixStore, MxsObject, SearchTerm, UserInfo, Vault}
import drivers.objectmatrix.{MxsMetadata, ObjectMatrixEntry, UserInfoBuilder}
import models.StorageEntry
import org.slf4j.LoggerFactory

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * TODO:
  *  - implement versions in the protocol
  *  - implement no-write-lock in the protocol
  * @param storageRef [[StorageEntry]] instance that this driver instance is assocaited with
  * @param mat implicitly provided ActorMaterializer
  */
class MatrixStoreDriver(override val storageRef: StorageEntry)(implicit val mat:Materializer) extends StorageDriver {
  private val logger = LoggerFactory.getLogger(getClass)
  lazy val userInfo = UserInfoBuilder()
    .withAddresses(storageRef.host.get)
    .withVault(storageRef.rootpath.get)
    .withUser(storageRef.user.get)
    .withPassword(storageRef.password.get)
    .getUserInfo

  /**
    * wrapper to perform an operation with a vault pointer and ensure that it is disposed when completed
    * @param blk block to perform operation. This is passed a Vault pointer, and can return anything. The wrapper returns the
    *            value of the block wrapped in a Try indicating whether the operation succeeded or failed; the vault is disposed
    *            either way
    * @tparam A type of return value of the block
    * @return
    */
  def withVault[A](blk:Vault=>Try[A]):Try[A] = {
    if(userInfo.isFailure) {
      logger.error(s"Could not open matrixstore for $storageRef: ", userInfo.failed.get)
      Failure(userInfo.failed.get)
    }

    val vault = MatrixStore.openVault(userInfo.get)
    try {
      val result = blk(vault)
      result
    } catch {
      case err:Throwable=>
        logger.error(s"Could not complete vault operation: ", err)
        Failure(err)
    } finally {
      vault.dispose()
    }
  }

  def withObject[A](vault:Vault,oid:String)(blk:MxsObject=>Try[A]):Try[A] = {
    Try {
      val mxsObj = vault.getObject(oid)
      blk(mxsObj)
    }.flatten
  }

  /**
    * utility function to directly copy from one stream to another
    * @param input InputStream to read from
    * @param output OutputStream to write to
    * @param bufferSize size of the temporary buffer to use.
    * @return the number of bytes written as a Long. Raises exceptions on failure (assumed it's within a try/catch block)
    */
  def copyStream(input:InputStream, output:OutputStream, bufferSize:Int) = {
    val buf=ByteBuffer.allocate(bufferSize)
    var bytesRead: Int = 0
    var totalRead: Long = 0

    try {
      do {
        bytesRead = input.read(buf.array())
        totalRead += bytesRead
        buf.flip()
        output.write(buf.array())
        buf.clear()
      } while (bytesRead > 0)
      totalRead
    } catch {
      case eof:EOFException=>
        logger.debug(s"Stream copy reached EOF")
        totalRead
    }
  }

  /**
    * Directly write an InputStream to the given path, until EOF (blocking)
    * @param path [[String]] absolute path to write
    * @param dataStream [[java.io.FileInputStream]] to write from
    */
  def writeDataToPath(path:String, dataStream:InputStream):Try[Unit] = withVault { vault=>
    val mxsFile = lookupPath(vault, path) match {
      case None=>
        val fileMeta = newFileMeta(path, -1)
        vault.createObject(fileMeta.toAttributes.toArray)
      case Some(oid)=>
        vault.getObject(oid)
    }

    val stream = mxsFile.newOutputStream()
    try {
      val copiedSize = copyStream(dataStream, stream, 10*1024*1024)
      val updatedFileMeta = newFileMeta(path, copiedSize)
      val vw = mxsFile.getAttributeView
      vw.writeAllAttributes(updatedFileMeta.toAttributes.asJavaCollection)
      Success()
    } catch {
      case err:Throwable=>
        logger.error(s"Could not copy file: ", err)
        Failure(err)
    } finally {
      stream.close()
    }
  }

  /**
    * returns the file extension of the provided filename, or None if there is no extension
    * @param fileNameString filename string
    * @return the content of the last extension
    */
  def getFileExt(fileNameString:String):Option[String] = {
    val re = ".*\\.([^\\.]+)$".r

    fileNameString match {
      case re(xtn) =>
        if (xtn.length < 8) {
          Some(xtn)
        } else {
          logger.warn(s"$xtn does not look like a file extension (too long), assuming no actual extension")
          None
        }
      case _ => None
    }
  }

  def newFileMeta(path:String, length:Long) = {
    val currentTime = ZonedDateTime.now()

    MxsMetadata(
      stringValues = Map(
        "MXFS_FILENAME_UPPER" -> path.toUpperCase,
        "MXFS_FILENAME"->path,
        "MXFS_PATH"->path.toString,
        "MXFS_MIMETYPE"->"application/octet-stream",
        "MXFS_DESCRIPTION"->s"File $path",
        "MXFS_PARENTOID"->"",
        "MXFS_FILEEXT"->getFileExt(path).getOrElse("")
      ),
      boolValues = Map(
        "MXFS_INTRASH"->false,
      ),
      longValues = Map(
        "DPSP_SIZE"->length,
        "MXFS_MODIFICATION_TIME"->currentTime.toInstant.toEpochMilli,
        "MXFS_CREATION_TIME"->currentTime.toInstant.toEpochMilli,
        "MXFS_ACCESS_TIME"->currentTime.toInstant.toEpochMilli,
      ),
      intValues = Map(
        "MXFS_CREATIONDAY"->currentTime.getDayOfMonth,
        "MXFS_COMPATIBLE"->1,
        "MXFS_CREATIONMONTH"->currentTime.getMonthValue,
        "MXFS_CREATIONYEAR"->currentTime.getYear,
        "MXFS_CATEGORY"->4  //set type to "document"
      )
    )
  }

  /**
    * Directly write a byte array to the given path (blocking)
    * @param path [[String]] absolute path to write
    * @param data [[Array]] (of bytes) -  byte array to output
    * @return a Try indicating success or failure. If successful the Try has a unit value.
    */
  def writeDataToPath(path:String, data:Array[Byte]):Try[Unit] = withVault { vault=>
    val mxsFile = lookupPath(vault, path) match {
      case None=>
        val fileMeta = newFileMeta(path, data.length)
        vault.createObject(fileMeta.toAttributes.toArray)
      case Some(oid)=>
        vault.getObject(oid)
    }

    val stream = mxsFile.newOutputStream()
    try {
      stream.write(data)
      Success()
    } finally {
      stream.close()
    }
  }

  /**
    * Delete the file at the given path (blocking)
    * @param path [[String]] absolute path to delete
    * @return [[Boolean]] indicating whether the file was deleted or not.
    */
  def deleteFileAtPath(path:String):Boolean = withVault { vault=>
    lookupPath(vault, path) match {
      case None =>
        logger.error(s"No file to delete at $path on $storageRef")
        Success(false)
      case Some(oid) =>
        withObject(vault, oid) { mxsObject =>
          mxsObject.delete()
          Success(true)
        }
    }
  } match {
    case Success(result) => result
    case Failure(err) =>
      logger.error(s"Could not delete file at $path on $storageRef: ", err)
      false
  }

  /**
    * Get a relevant type of InputStream to read a file's data
    * @param path [[String]] Absolute path to open
    * @return [[java.io.InputStream]] subclass wrapped in a [[Try]]
    */
  def getReadStream(path:String):Try[InputStream] = withVault { vault=>
    lookupPath(vault, path) match {
      case None=>
        Failure(new RuntimeException(s"File $path does not exist"))
      case Some(oid)=>
        withObject(vault, oid) { mxsObject=>
        Success(mxsObject.newInputStream())
      }
    }
  }

  /**
    * Get a relevant type of OutputStream to write a file's data.  this may truncate the file.
    * @param path [[String]] Absolute path to open
    * @return [[java.io.OutputStream]] subclass wrapped in a [[Try]]
    */
  def getWriteStream(path:String):Try[OutputStream] = withVault { vault=>
    lookupPath(vault, path) match {
      case None=>
        Failure(new RuntimeException(s"File $path does not exist"))
      case Some(oid)=>
        withObject(vault, oid) { mxsObject=>
          Success(mxsObject.newOutputStream())
        }
    }
  }

  /**
    * Get a Map of metadata relevant to the specified file.  The contents can vary between implementations, but should always
    * have 'size (Long converted to String) and 'lastModified (Long converted to String) members
    * @param path [[String]] Absolute path to open
    * @return [[Map]] of [[Symbol]] -> [[String]] containing metadata about the given file.
    */
  def getMetadata(path:String):Map[Symbol,String] = {
    if(userInfo.isFailure) {
      logger.error(s"Can't look up $path on storage ${storageRef.id}: ", userInfo.failed.get)
      throw new RuntimeException("Could not access path")
    }

    val vault = MatrixStore.openVault(userInfo.get)

    val resultFuture = findByFilename(vault, path).map({
      case None=>
        Map('size->"-1")
      case Some(omEntry)=>
        val fileAttrKeysMap = omEntry.attributes.map(_.toSymbolMap)
        Map(
          'size->omEntry.fileAttribues.map(_.size).getOrElse(-1).toString,
          'lastModified->omEntry.fileAttribues.map(_.mtime).getOrElse(-1).toString,
        ) ++ fileAttrKeysMap.getOrElse(Map())
    })

    Await.result(resultFuture, 30.seconds)
  }

  def lookupPath(vault:Vault, fileName:String)  = {
    logger.debug(s"Lookup $fileName on ${vault.getId}")
    val searchTerm = SearchTerm.createSimpleTerm("MXFS_FILENAME", fileName) //FIXME: check the metadata field namee
    val iterator = vault.searchObjectsIterator(searchTerm, 1).asScala

    var finalSeq: Seq[String] = Seq()
    while (iterator.hasNext) { //the iterator contains the OID
      finalSeq ++= Seq(iterator.next())
    }
    if(finalSeq.length>1) logger.warn(s"Found ${finalSeq.length} object matching $fileName, only using the first")
    finalSeq.headOption
  }

  /**
    * locate files for the given filename, as stored in the metadata. This assumes that one or at most two records will
    * be returned and should therefore be more efficient than using the streaming interface. If many records are expected,
    * this will be inefficient and you should use the streaming interface.
    * this will return a Future to avoid blocking any other lookup requests that would hit the cache
    * @param fileName file name to search for
    * @return a Future, containing either a sequence of zero or more results as String oids or an error
    */
  def findByFilename(vault:Vault, fileName:String):Future[Option[ObjectMatrixEntry]] =
    lookupPath(vault, fileName) match {
      case Some(oid)=>ObjectMatrixEntry(oid).getMetadata(vault, mat, global).map(entry=>Some(entry))
      case None=>Future(None)
    }

  /**
    * Does the given path exist on this storage?
    * @param path
    * @return
    */
  def pathExists(path:String):Boolean =
    withVault { vault=>
      logger.debug(s"Lookup $path on ${vault.getId}")
      val searchTerm = SearchTerm.createSimpleTerm("MXFS_FILENAME", path) //FIXME: check the metadata field namee
      val iterator = vault.searchObjectsIterator(searchTerm, 1).asScala

      Success(iterator.hasNext)
    } match {
        case Success(result)=>result
        case Failure(err)=>throw err
    }

}
