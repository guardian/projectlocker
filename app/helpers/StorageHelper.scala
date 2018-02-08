package helpers

import java.io.{InputStream, OutputStream}

import drivers.StorageDriver
import models.FileEntry
import play.api.Logger

import scala.concurrent.Future
import scala.util.Try

object StorageHelper {
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

  def copyFile(sourceFile: FileEntry, destFile: FileEntry):Future[Either[Seq[String],Unit]] = {
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
        case Right(storageDrivers)=>
          val sourceStorageDriver = storageDrivers.head
          val destStorageDriver = storageDrivers(1)

          val sourceFullPath = futures(1).asInstanceOf[Seq[String]].head
          val destFullPath = futures(1).asInstanceOf[Seq[String]](1)

          Logger.info(s"Copying from $sourceFullPath on $sourceStorageDriver to $destFullPath on $destStorageDriver")

          val sourceStreamTry = sourceStorageDriver.getReadStream(sourceFullPath)
          val destStreamTry = destStorageDriver.getWriteStream(destFullPath)

          if(sourceStreamTry.isFailure || destStreamTry.isFailure){
            Left(Seq(sourceStreamTry.failed.getOrElse("").toString, destStreamTry.failed.getOrElse("").toString))
          } else {
            //safe, because we've already checked that neither failed
            try {
              val bytesCopied = copyStream(sourceStreamTry.get,destStreamTry.get)
              sourceStreamTry.get.close()
              destStreamTry.get.close()
              Right(bytesCopied)
            } catch {
              case ex:Throwable=>
                Left(Seq(ex.toString))
            } finally {
              sourceStreamTry.get.close()
              destStreamTry.get.close()
            }
          }
      }
    })

    bytesCopiedFuture.map({
      case Left(errors)=>Left(errors)
      case Right(bytesCopied)=>
        Logger.debug(s"Copied $bytesCopied bytes")
        //need to check if the number of bytes copied is the same as the source file. If so return Right() otherwise Left()
    })
  }
}
