package controllers

import com.google.inject.Inject
import play.api.Configuration
import play.api.db.Database
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import models._

class ProjectTemplate @Inject() (config: Configuration, db: Database) extends Controller {

  def editTemplate = Action {
    val editForm = Form(
      mapping(
        "name"->text,
        "projectType"->mapping(
          "name"->text,
          "opensWith"->text,
          "targetVersion"->text
        )(ProjectType.apply)(ProjectType.unapply),
        "sourceDir"->mapping(
          "filepath"->text,
          "storageType"->text,
          "user"->text,
          "isDir"->boolean,
          "ctime"->jodaDate,
          "mtime"->jodaDate,
          "atime"->jodaDate
        )(FileEntry.apply)(FileEntry.unapply)
      )(ProjectTemplate.apply)(ProjectTemplate.unapply)
    )
    Ok(views.html.template_edit_form("New project template")(editForm))
  }

  def projectTemplateSubmit = Action {
    Ok("")
  }
}
