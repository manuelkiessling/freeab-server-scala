package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def save = Action {
    Ok(Json.toJson(Map("success" -> true)))
  }

}