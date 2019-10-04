package drivers.objectmatrix

import java.time.{Instant, ZoneId, ZonedDateTime}

import com.om.mxs.client.japi.MXFSFileAttributes

case class FileAttributes(fileKey:AnyRef, name:String, parent:String, isDir:Boolean, isOther:Boolean, isRegular:Boolean, isSymlink:Boolean, ctime:ZonedDateTime, mtime:ZonedDateTime, atime:ZonedDateTime, size:Long)

object FileAttributes {
  def apply(from:MXFSFileAttributes) = new FileAttributes(
    from.fileKey(),
    from.getName,
    from.getParent,
    from.isDirectory,
    from.isOther,
    from.isRegularFile,
    from.isSymbolicLink,
    ZonedDateTime.ofInstant(Instant.ofEpochMilli(from.creationTime()), ZoneId.systemDefault()),
    ZonedDateTime.ofInstant(Instant.ofEpochMilli(from.lastModifiedTime()), ZoneId.systemDefault()),
    ZonedDateTime.ofInstant(Instant.ofEpochMilli(from.lastAccessTime()), ZoneId.systemDefault()),
    from.size()
  )
}
