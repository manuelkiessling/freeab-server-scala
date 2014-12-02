package models

case class FormExperiment(
  name: String,
  scope: Double,
  formVariations: List[FormVariation]
)