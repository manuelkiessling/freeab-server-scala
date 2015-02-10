package models

import java.util.UUID

import anorm.SqlParser._
import anorm.{RowParser, SQL, ~}
import play.api.Play.current
import play.api.db.DB

case class Variation(
  id: String,
  experimentId: String,
  name: String,
  weight: Double
)

object Variation {

  val variationParser: RowParser[Variation] = {
    str("id") ~
    str("experiment_id") ~
    str("name") ~
    double("weight") map {
      case id ~ experimentId ~ name ~ weight => Variation(id, experimentId, name, weight)
    }
  }

  def add(formVariation: FormVariation, experimentId: String): Option[Variation] = DB.withConnection { implicit connection =>
    val vaId = UUID.randomUUID().toString()
    val result = SQL(
      """INSERT INTO variations (experiment_id, name, weight) VALUES ({experimentId}, {name}, {scope})""").on(
        "experimentId" -> experimentId,
        "name" -> formVariation.name,
        "weight" -> formVariation.weight
      ).executeInsert()

    result match {
      case Some(id) => Option[Variation](Variation(id.toString, experimentId, formVariation.name, formVariation.weight))
      case None => None
    }
  }

}