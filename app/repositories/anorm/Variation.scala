package repositories.anorm

import java.util.UUID
import anorm.SqlParser._
import anorm._
import models.{FormVariation}
import play.api.db.DB
import play.api.Play.current

class Variation(dbConfigName: String) {

  val variationParser: RowParser[models.Variation] = {
    str("id") ~
      str("experiment_id") ~
      str("name") ~
      double("weight") map {
      case id ~ experimentId ~ name ~ weight => models.Variation(id, experimentId, name, weight)
    }
  }
  
  def add(formVariation: FormVariation, experimentId: String) = DB.withConnection(dbConfigName) { implicit connection =>
    val variationId = UUID.randomUUID().toString()
    val result = SQL(
      """INSERT INTO variations (id, experiment_id, name, weight) VALUES ({variationId}, {experimentId}, {name}, {weight})""")
      .on(
        "variationId" -> variationId,
        "experimentId" -> experimentId,
        "name" -> formVariation.name,
        "weight" -> formVariation.weight.toFloat
      ).execute()
    // Using executeInsert() results in an SQLException with message "statement is not executing", although it actually executes
    // See http://stackoverflow.com/a/25816619
    // TODO: We should handle errors/exceptions right here
  }
  
}
