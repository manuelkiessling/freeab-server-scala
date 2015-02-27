package controllers.api

import models.{FormExperiment, FormVariation}
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.mvc._
import play.api.libs.functional.syntax._

object Experiments extends Controller {

  private def areVariationsNamesUnique_?(formVariations: List[FormVariation]): Boolean = {
    formVariations.map(_.name).distinct.size == formVariations.size
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

  // TODO: Find a way to inject those
  private lazy val variationRepository = new repositories.anorm.Variation("api")
  private lazy val experimentRepository = new repositories.anorm.Experiment("api", variationRepository)
  
  implicit val formVariationReads: Reads[FormVariation] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "weight").read[Double]
    )(FormVariation.apply _)

  // It might actually be simpler to verify correctness after transforming the JSON,
  // but I want correctness as early as possible. There should never exist an invalid
  // model during runtime
  implicit val formExperimentReads: Reads[FormExperiment] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "scope").read[Double] and
      (JsPath \ "variations").read[List[FormVariation]]
        .filter(ValidationError("Variation names must be unique"))(areVariationsNamesUnique_?)
        .filter(ValidationError("The sum of the variation weights must be 100.0"))(_.map(_.weight).sum == 100.0)
    )(FormExperiment.apply _)

  def save = Action(BodyParsers.parse.json) { request =>

    val formExperimentResult = request.body.validate[FormExperiment]

    formExperimentResult match {
      case e: JsError => {
        // This concatenates all error messages of all validation errors into one string
        // e.errors is a Seq[(JsPath, Seq[ValidationError(message: String, args: Any*)])]
        // This means we pull out each message of each ValidationError in each Seq of ValidationErrors in
        // the Seq of errors
        badRequest(e.errors.flatMap(_._2.map(_.message)) mkString ". ")
      }
      case s: JsSuccess[FormExperiment] => {
        val formExperiment = s.get
        val (experimentOption, errorMessage) = experimentRepository.add(formExperiment)
        experimentOption.map( experiment =>
          Ok(Json.toJson(Map("success" -> JsBoolean(true), "experimentId" -> JsString(experiment.id))))
        ) getOrElse badRequest(errorMessage getOrElse "Unknown error")
      }
    }
  }

}