package models

import org.joda.time.DateTime

/**
  * Created by localhome on 12/01/2017.
  */
case class FileEntry(filepath: String, storageType: String, user:String,
                     isDir: Boolean,
                     ctime: DateTime, mtime: DateTime, atime: DateTime) {

}
