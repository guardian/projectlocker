package postrun

import helpers.PostrunDataCache
import models.{PlutoCommission, PlutoWorkingGroup, ProjectEntry, ProjectType}

import scala.concurrent.Future
import scala.util.Try

trait PojoPostrun {
  def postrun(projectFileName:String,projectEntry:ProjectEntry,projectType:ProjectType,dataCache:PostrunDataCache,
              workingGroupMaybe: Option[PlutoWorkingGroup], commissionMaybe: Option[PlutoCommission]):Future[Try[PostrunDataCache]]
}
