package drivers

import java.io._

import scala.concurrent.Future
import scala.io.Source
import scala.util.Try

/**
  * Protocol for StorageDriver classes.  All valid StorageDrivers must extend this trait.
  */
trait StorageDriver {
  val storageRef:models.StorageEntry

  /**
    * Directly write an InputStream to the given path, until EOF (blocking)
    * FIXME: this should not be in the protocol as it is specific to [[java.io.FileInputStream]]
    * @param path [[String]] absolute path to write
    * @param dataStream [[java.io.FileInputStream]] to write from
    */
  def writeDataToPath(path:String, dataStream:FileInputStream):Try[Unit]

  /**
    * Directly write a byte array to the given path (blocking)
    * @param path [[String]] absolute path to write
    * @param data [[Array]] (of bytes) -  byte array to output
    * @return a Try indicating success or failure. If successful the Try has a unit value.
    */
  def writeDataToPath(path:String, data:Array[Byte]):Try[Unit]

  /**
    * Delete the file at the given path (blocking)
    * @param path [[String]] absolute path to delete
    * @return [[Boolean]] indicating whether the file was deleted or not.
    */
  def deleteFileAtPath(path:String):Boolean

  /**
    * Get a relevant type of InputStream to read a file's data
    * @param path [[String]] Absolute path to open
    * @return [[java.io.InputStream]] subclass wrapped in a [[Try]]
    */
  def getReadStream(path:String):Try[InputStream]

  /**
    * Get a relevant type of OutputStream to write a file's data.  this may truncate the file.
    * @param path [[String]] Absolute path to open
    * @return [[java.io.OutputStream]] subclass wrapped in a [[Try]]
    */
  def getWriteStream(path:String):Try[OutputStream]

  /**
    * Get a Map of metadata relevant to the specified file.  The contents can vary between implementations, but should always
    * have 'size (Long converted to String) and 'lastModified (Long converted to String) members
    * @param path [[String]] Absolute path to open
    * @return [[Map]] of [[Symbol]] -> [[String]] containing metadata about the given file.
    */
  def getMetadata(path:String):Map[Symbol,String]
}
