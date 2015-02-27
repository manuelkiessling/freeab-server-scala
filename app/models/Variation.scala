package models

final case class Variation(
  id: String,
  experimentId: String,
  name: String,
  weight: Double
)