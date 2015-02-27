package models

final case class FormExperiment(
  name: String,
  scope: Double,
  formVariations: List[FormVariation]
)