package postrun
import java.io.File

import helpers.PostrunDataCache
import models.{PlutoCommission, PlutoWorkingGroup, ProjectEntry, ProjectType}
import org.apache.commons.io.FileUtils

import sys.process._
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

import scala.concurrent.ExecutionContext.Implicits.global

class RunXmlLint extends PojoPostrun {
  override def postrun(projectFileName: String, projectEntry: ProjectEntry, projectType: ProjectType, dataCache: PostrunDataCache, workingGroupMaybe: Option[PlutoWorkingGroup], commissionMaybe: Option[PlutoCommission]): Future[Try[PostrunDataCache]] = Future {
    val originalFile = new File(projectFileName + ".orig")
    val destFileUncompressed = new File(projectFileName + ".ungz")
    val destFile = new File(projectFileName)

    try {
      FileUtils.moveFile(destFile, originalFile)

      val resultCode = ("zcat" #< originalFile #| "xmllint --format -" #> destFileUncompressed).!
      if (resultCode != 0) {
        FileUtils.moveFile(originalFile, destFile)
        Failure(new RuntimeException(s"xmllint chain returned $resultCode"))
      } else {
        val gzipResultCode = ("gzip" #< destFileUncompressed #> destFile).!
        if(gzipResultCode !=0){
          FileUtils.moveFile(originalFile, destFile)
          Failure(new RuntimeException(s"gzip returned $resultCode"))
        }
        FileUtils.forceDelete(originalFile)
        FileUtils.forceDelete(destFileUncompressed)
        Success(dataCache)
      }
    } catch {
      case e:Throwable=>Failure(e)
    }
  }
}
