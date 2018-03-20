package helpers

import java.io.{InputStream, OutputStream}

import drivers.StorageDriver
import models.FileEntry
import play.api.Logger

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

class StorageHelper {
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
                                 sourceFullPath:String, destFullPath: String)  = {
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
          Right(Tuple2(bytesCopied,sourceStorageDriver.getMetadata(sourceFullPath)))
      } catch {
        case ex:Throwable=>
          Left(Seq(ex.toString))
      } finally {
        sourceStreamTry.get.close()
        destStreamTry.get.close()
      }
    }
  }

  def deleteFile(targetFile: FileEntry)(implicit db:slick.jdbc.PostgresProfile#Backend#Database):Future[Boolean] = {
    targetFile.storage.map({
      case Some(storageEntry)=>
        storageEntry.getStorageDriver match {
          case Some(storageDriver)=>
            storageDriver.deleteFileAtPath(targetFile.getFullPath.toString)
          case None=>
            logger.error(s"Can't delete file at ${targetFile.getFullPath} because storage $storageEntry has no storage driver")
            false
        }
      case None=>
        logger.error(s"Can't delete file at ${targetFile.getFullPath} because file record has no storage")
        false
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

          val sourceStreamTry = sourceStorageDriver.getReadStream(sourceFullPath)
          val destStreamTry = destStorageDriver.getWriteStream(destFullPath)

          doByteCopy(sourceStorageDriver,sourceStreamTry,destStreamTry,sourceFullPath,destFullPath)
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
          Right(Unit)
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
}
