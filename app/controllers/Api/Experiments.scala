package controllers.Api

import org.h2.jdbc.JdbcSQLException
import play.api.libs.json._
import play.api.mvc._

object Experiments extends Controller {

  def save = Action {
    val formExperiment = models.FormExperiment("foo", 50.0f)
    try {
      models.Experiment.add(formExperiment) match {
        case Some(experiment) => {
          Ok(Json.toJson(Map("success" -> JsBoolean(true), "experimentId" -> JsNumber(experiment.id))))
        }
        case None => InternalServerError
      }
    } catch {
      case jse: JdbcSQLException => BadRequest(
        Json.toJson(
          Map(
            "success" -> JsBoolean(false),
            "error" -> Json.toJson(
              Map(
                "message" -> "Bad Request",
                "detail" -> "An experiment with this name already exists"
              )
            )
          )
        )
      )
    }
  }

}