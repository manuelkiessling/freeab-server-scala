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

  // Background info on Reads is at https://www.playframework.com/documentation/2.1.1/ScalaJsonCombinators
  // and https://www.playframework.com/documentation/2.3.x/ScalaJsonCombinators
  implicit val formVariationReads: Reads[FormVariation] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "weight").read[Double]
    )(FormVariation)

  // It might actually be simpler to verify correctness after transforming the JSON,
  // but I want correctness as early as possible. There should never exist an invalid
  // model during runtime
  implicit val formExperimentReads: Reads[FormExperiment] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "scope").read[Double] and
      (JsPath \ "variations").read[List[FormVariation]]
        .filter(ValidationError("An experiment needs at least 2 variations"))(_.size > 1)
        .filter(ValidationError("Variation names must be unique"))(areVariationsNamesUnique_?)
        .filter(ValidationError("The sum of the variation weights must be 100.0"))(_.map(_.weight).sum == 100.0)
    )(FormExperiment)

  def save = Action(BodyParsers.parse.json) { request =>

    val formExperimentResult = request.body.validate[FormExperiment]

    formExperimentResult match {
      case e: JsError => {
        // Here, e.errors is a
        // List(
        //       (
        //         JsPath,
        //         List(
        //               ValidationError(message, WrappedArray())
        //             )
        //       )
        //     )
        //
        // e.g.:
        //
        // List(
        //       (
        //         /variations,
        //         List(
        //               ValidationError(The sum of the variation weights must be 100.0, WrappedArray())
        //             )
        //       )
        //     )

        // TODO: There has to be a better way!
        val firstJsPathWithProblem = e.errors(0)._1
        firstJsPathWithProblem.path(0) match {
          case KeyPathNode("name") => badRequest("An experiment needs a name")
          case _ => {
            // This concatenates all error messages of all validation errors into one string
            // by pulling out each message of each ValidationError in each List of ValidationErrors in
            // the List of all errors
            badRequest(e.errors.flatMap(_._2.map(_.message)) mkString ". ")
          }
        }
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