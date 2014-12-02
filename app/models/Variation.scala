package models

import anorm.SqlParser._
import anorm.{RowParser, SQL, ~}
import play.api.Play.current
import play.api.db.DB

case class Variation(
  id: Int,
  experimentId: Int,
  name: String,
  weight: Double
)

object Variation {

  val variationParser: RowParser[Variation] = {
    int("id") ~
    int("experimentId") ~
    str("name") ~
    double("weight") map {
      case id ~ experimentId ~ name ~ weight => Variation(id, experimentId, name, weight)
    }
  }

}