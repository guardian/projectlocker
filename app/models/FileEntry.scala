package models

import com.google.inject.Inject
import org.joda.time.DateTime
import play.api.db.Database
import play.api.libs.openid.Errors.AUTH_CANCEL

/**
  * Created by localhome on 12/01/2017.
  */

class FileEntryFactory @Inject() (db:Database)
{
  def buildQuerySql(filepath: Option[String], storageType: Option[String], user: Option[String], isDir: Option[Boolean]):String = {
    val parts = List(
      filepath match {
        case _ => "filepath=" + _
        case None=>""
      },
      storageType match {
        case _=>"storageType="+_
        case None=>""
      }
    )
    "where " + parts.reduce( (acc:String,item:String)=>{acc + " and " + item})

  }

  def lookup(filepath: Option[String], storageType: Option[String], user: Option[String], isDir: Option[Boolean]):Option[List[FileEntry]] = {
    val conn = db.getConnection()

    try {
      val stmt = conn.createStatement
      val rs = stmt.executeQuery()

      while (rs.next()) {
        outString += rs.getString("testkey")
      }
    } finally {
      conn.close()
    }
  }
}

case class FileEntry(filepath: String, storageType: String, user:String,
                     isDir: Boolean,
                     ctime: DateTime, mtime: DateTime, atime: DateTime) {
  def save(db:Database):Unit = {

  }
}
