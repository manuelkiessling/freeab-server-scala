package controllers

import play.api.mvc._
import play.api.libs.json._

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def save = Action {
    val formExperiment = models.FormExperiment("foo", 50.0f)
    models.Experiment.add(formExperiment) match {
      case Some(experiment) => {
        Ok(Json.toJson(Map("success" -> JsBoolean(true), "experimentId" -> JsNumber(experiment.id))))
      }
      case None => InternalServerError
    }
  }

}