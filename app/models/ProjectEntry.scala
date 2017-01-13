package models

import org.joda.time.DateTime

/**
  * Created by localhome on 12/01/2017.
  */
case class ProjectEntry (files: List[FileEntry], projectType: ProjectType, created:DateTime, user: String) {

}
