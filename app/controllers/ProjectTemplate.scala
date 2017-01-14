package controllers

import com.google.inject.Inject
import play.api.Configuration
import play.api.mvc._
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile

class ProjectTemplate @Inject() (config: Configuration, dbConfigProvider: DatabaseConfigProvider) extends Controller {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  def projectTemplateSubmit = Action {
    Ok("")
  }
}
