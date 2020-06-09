package helpers

import java.io.{InputStream, OutputStream}

import akka.stream.Materializer
import drivers.StorageDriver
import javax.inject.Inject
import models.{FileEntry, StorageEntry}
import play.api.Logger
import org.slf4j.MDC

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

class StorageHelper @Inject() (implicit mat:Materializer) {
  val logger: Logger = Logger(this.getClass)
  /**
    * Internal method to copy from one stream to another, independent of the stream implementation.
    * Note that this method is blocking.
    * @param source [[java.io.InputStream]] instance to copy from
    * @param dest [[java.io.OutputStream]] instance to copy to
    * @param chunkSize [[Int]] representing the amount of data to be buffered in memory. Defaults to 2Kbyte.
    * @return [[Int]] representing the number of bytes copied
    */
  def copyStream(source: InputStream,dest: OutputStream, chunkSize:Int=2048):Int = {
    var count = -1
    var total = 0
    val buffer = Array.ofDim[Byte](chunkSize)

    while({count = source.read(buffer); count>0}){
      dest.write(buffer,0,count)
      total+=count
    }
    total
  }

  final protected def doByteCopy(sourceStorageDriver:StorageDriver, sourceStreamTry:Try[InputStream], destStreamTry:Try[OutputStream],
                                 sourceFullPath:String, sourceVersion:Int, destFullPath: String)  = {
    if(sourceStreamTry.isFailure || destStreamTry.isFailure){
      Left(Seq(sourceStreamTry.failed.getOrElse("").toString, destStreamTry.failed.getOrElse("").toString))
    } else {
      //safe, because we've already checked that neither Try failed
      try {
        val bytesCopied = copyStream(sourceStreamTry.get,destStreamTry.get)
        logger.debug(s"copied $sourceFullPath to $destFullPath: $bytesCopied bytes")
        sourceStreamTry.get.close()
        destStreamTry.get.close()
        if(bytesCopied==0)
          Left(Seq(s"could not copy $sourceFullPath to $destFullPath - empty file"))
        else
          Right(Tuple2(bytesCopied,sourceStorageDriver.getMetadata(sourceFullPath, sourceVersion)))
      } catch {
        case ex:Throwable=>
          Left(Seq(ex.toString))
      } finally {
        sourceStreamTry.get.close()
        destStreamTry.get.close()
      }
    }
  }

  def deleteFile(targetFile: FileEntry)(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Either[String, FileEntry]] = {
    val futures = Future.sequence(Seq(targetFile.storage, targetFile.getFullPath))

    futures.map(results=>{
      val storageResult = results.head.asInstanceOf[Option[StorageEntry]]
      MDC.put("storageResult", storageResult.toString)
      val fullPath = results(1).asInstanceOf[String]
      MDC.put("fullPath", fullPath)
      storageResult match {
        case Some(storageEntry) =>
          storageEntry.getStorageDriver match {
            case Some(storageDriver) =>
              MDC.put("storageDriver", storageDriver.toString)
              storageDriver.deleteFileAtPath(fullPath, targetFile.version) match {
                case true=>
                  val updatedFileEntry = targetFile.copy(hasContent = false)
                  updatedFileEntry.save
                  Right(updatedFileEntry)
                case false=>
                  Left("storage driver failed to delete file")
              }

            case None =>
              logger.error(s"Can't delete file at $fullPath because storage $storageEntry has no storage driver")
              Left("No storage driver configured, enable debugging for helpers.StorageHelper for more info")
          }
        case None =>
          logger.error(s"Can't delete file at $fullPath because file record has no storage")
          Left("No storage reference on record, enable debugging for helpers.StorageHelper for more info")
      }
    })
  }

  /**
    * Copies from the file represented by sourceFile to the (non-existing) file represented by destFile.
    * Both should have been saved to the database before calling this method.  The files do not need to be on the same
    * storage type
    * @param sourceFile - [[models.FileEntry]] instance representing file to copy from
    * @param destFile - [[models.FileEntry]] instance representing file to copy to
    * @param db - database instance, usually passed implicitly.
    * @return [[Future[Either[Seq[String],FileEntry]] - a future containing either a list of strings giving failure reasons or a
    *        new, updated [[models.FileEntry]] representing @destFile
    */
  def copyFile(sourceFile: FileEntry, destFile: FileEntry)
              (implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Either[Seq[String],FileEntry]] = {
    val storageDriversFuture = Future.sequence(Seq(sourceFile.storage,destFile.storage)).map(results=>{
      val successfulResults = results.flatten.flatMap(_.getStorageDriver)

      if(successfulResults.length==2){
        Right(successfulResults)
      } else {
        MDC.put("sourceFile", sourceFile.toString)
        MDC.put("destFile", destFile.toString)
        logger.debug(s"sourceFile: ${sourceFile.toString}")
        logger.debug(s"destFile: ${destFile.toString}")
        Left(Seq("Either source or destination was missing a storage or a storage driver"))
      }
    })

    val actualFilenamesFuture = Future.sequence(Seq(sourceFile.getFullPath,destFile.getFullPath))

    val bytesCopiedFuture = Future.sequence(Seq(storageDriversFuture, actualFilenamesFuture)).map(futures=>{
      val storageDrivers = futures.head.asInstanceOf[Either[Seq[String],Seq[StorageDriver]]]

      storageDrivers match {
        case Left(errors)=>Left(errors)
        case Right(actualStorageDrivers)=>
          val sourceStorageDriver = actualStorageDrivers.head
          val destStorageDriver = actualStorageDrivers(1)

          val sourceFullPath = futures(1).asInstanceOf[Seq[String]].head
          val destFullPath = futures(1).asInstanceOf[Seq[String]](1)

          logger.info(s"Copying from $sourceFullPath on $sourceStorageDriver to $destFullPath on $destStorageDriver")

          val sourceStreamTry = sourceStorageDriver.getReadStream(sourceFullPath, sourceFile.version)
          val destStreamTry = destStorageDriver.getWriteStream(destFullPath, destFile.version)

          doByteCopy(sourceStorageDriver,sourceStreamTry,destStreamTry,sourceFullPath, sourceFile.version, destFullPath)
      }
    })

    val checkFuture = bytesCopiedFuture.map({
      case Left(errors)=>Left(errors)
      case Right((bytesCopied,metaDict))=>
        logger.debug(s"Copied $bytesCopied bytes")
        //need to check if the number of bytes copied is the same as the source file. If so return Right() otherwise Left()
        val fileSize = metaDict('size).toLong
        logger.debug(s"Destination size is $fileSize")
        if(bytesCopied!=fileSize){
          Left(Seq(s"Copied file byte size $bytesCopied did not match source file $fileSize"))
        } else {
          Right( () )
        }
    })

    checkFuture.flatMap({
      case Left(errors)=>Future(Left(errors))
      case Right(nothing)=>
        destFile.updateFileHasContent.map({
          case Success(rowsUpdated)=>
            Right(destFile.copy(hasContent = true))
          case Failure(error)=>
            Left(Seq(error.toString))
        })
    })
  }

  def findFile(targetFile: FileEntry)(implicit db:slick.jdbc.PostgresProfile#Backend#Database) = {
    val futures = Future.sequence(Seq(targetFile.storage, targetFile.getFullPath))

    futures.map(futureResults=>{
      val maybeStorage = futureResults.head.asInstanceOf[Option[StorageEntry]]
      val fullPath = futureResults(1).asInstanceOf[String]

      val maybeStorageDriver = maybeStorage.flatMap(_.getStorageDriver)

      maybeStorageDriver match {
        case Some(storageDriver)=>
          storageDriver.pathExists(targetFile.filepath, targetFile.version)
        case None=>
          throw new RuntimeException(s"No storage driver defined for ${maybeStorage.map(_.repr).getOrElse("unknown storage")}")
      }
    })
  }

  def onStorageMetadata(targetFile: FileEntry)(implicit db:slick.jdbc.PostgresProfile#Backend#Database) = {
    targetFile.storage.map(maybeStorage=>{
      val maybeStorageDriver = maybeStorage.flatMap(_.getStorageDriver)

      maybeStorageDriver match {
        case Some(storageDriver)=>
          storageDriver.getMetadata(targetFile.filepath, targetFile.version)
        case None=>
          throw new RuntimeException(s"No storage driver defined for ${maybeStorage.map(_.repr).getOrElse("unknown storage")}")
      }
    })
  }
}
