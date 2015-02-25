package controllers.api

import java.sql.SQLException
import models.{FormExperiment, FormVariation}
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.mvc._
import play.api.libs.functional.syntax._

object Experiments extends Controller {

  private def variationsSum(xs: List[FormVariation]): Double = {
    if (xs.isEmpty) 0.0
    else xs.head.weight + variationsSum(xs.tail)
  }
  
  private def areVariationsNamesUnique_?(formVariations: List[FormVariation]): Boolean = {
    // See http://stackoverflow.com/a/3912833
    // groupBy creates a Map[String,List[FormVariation]] where each distinct FormVariation.name points
    // to a List of all FormVariations that have this name
    // map(_._2.head) returns:
    // - a list of each Map entry ( _ ), and for this
    // - an Iterable[List[FormVariation]] where each entry is a List of the FormVariations that have this name ( _2 ),
    // - and for each of there Lists the first ("head") entry
    // If the sizes of this filtered List and the original List are not equal, then there was at least one redundant
    // FormVariation.name in the List of FormVariations
    formVariations.groupBy(_.name).map(_._2.head).size == formVariations.size
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

  // @TODO: Find a way to inject those
  private lazy val variationRepository = new repositories.anorm.Variation("api")
  private lazy val experimentRepository = new repositories.anorm.Experiment("api", variationRepository)
  
  // @TODO: Map to case class directly, see https://www.playframework.com/documentation/2.3.x/ScalaJsonCombinators
  implicit val formVariationReads: Reads[FormVariation] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "weight").read[Double]
    )(FormVariation.apply _)

  implicit val formExperimentReads: Reads[FormExperiment] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "scope").read[Double] and
      (JsPath \ "variations").read[List[FormVariation]]
        .filter(ValidationError("Variation names must be unique"))(areVariationsNamesUnique_?)
    )(FormExperiment.apply _)

  def save = Action(BodyParsers.parse.json) { request =>

    val formExperimentResult = request.body.validate[FormExperiment]

    formExperimentResult match {
      case e: JsError => {
        // This concatenates all error messages of all validation errors into one string
        // e.errors is a Seq[(JsPath, Seq[ValidationError(message: String, args: Any*)])]
        // This means we pull out each message of each ValidationError in each Seq of ValidationErrors in
        // the Seq of errors
        badRequest(e.errors.map(_._2.map(_.message)).flatten mkString ". ")
      }
      case s: JsSuccess[FormExperiment] => {
        val formExperiment = s.get
        if (variationsSum(formExperiment.formVariations) != 100.0) { // TODO: Check via JSON Reads, too. Filters can be concatenated I think
          badRequest("The sum of the variation weights must be 100.0")
        } else {
          val (experimentOption, errorMessage) = experimentRepository.add(formExperiment)
          experimentOption.map( experiment =>
            Ok(Json.toJson(Map("success" -> JsBoolean(true), "experimentId" -> JsString(experiment.id))))
          ) getOrElse badRequest(errorMessage getOrElse "Unknown error")
        }
      }
    }
  }

}