package controllers

import play.api._
import play.api.mvc._

class Application extends Controller {

  def index(path:String) = Action {
    Ok(views.html.index())
  }

}