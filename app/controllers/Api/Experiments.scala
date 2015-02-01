package controllers.Api

import models.{FormExperiment, FormVariation}
import org.h2.jdbc.JdbcSQLException
import play.api.libs.json._
import play.api.mvc._
import play.api.libs.functional.syntax._

object Experiments extends Controller {

  private def variationsSum(xs: List[FormVariation]): Double = {
    if (xs.isEmpty) 0.0
    else xs.head.weight + variationsSum(xs.tail)
  }
  
  private def badRequest(detail: String) = {
    BadRequest(
      Json.toJson(
        Map(
          "success" -> JsBoolean(false),
          "error" -> Json.toJson(
            Map(
              "message" -> "Bad Request",
              "detail" -> detail
            )
          )
        )
      )
    )
  }

  implicit val formVariationReads: Reads[FormVariation] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "weight").read[Double]
    )(FormVariation.apply _)

  implicit val formExperimentReads: Reads[FormExperiment] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "scope").read[Double] and
      (JsPath \ "variations").read[List[FormVariation]]
    )(FormExperiment.apply _)

  def save = Action(BodyParsers.parse.json) { request =>

    val formExperimentResult = request.body.validate[FormExperiment]

    formExperimentResult.fold(

      errors => {
        badRequest("The structure of your request body could not be understood")
      },

      formExperiment => {
        if (variationsSum(formExperiment.formVariations) != 100.0) {
          badRequest("The sum of the variation weights must be 100.0")
        } else {
          try {
            val experimentOption = models.Experiment.add(formExperiment)
            experimentOption.map( experiment =>
              Ok(Json.toJson(Map("success" -> JsBoolean(true), "experimentId" -> JsNumber(experiment.id))))
            ) getOrElse InternalServerError
          } catch {
            case jse: JdbcSQLException => {
              badRequest("An experiment with this name already exists")
            }
          }
        }
      }

    )

  }

}