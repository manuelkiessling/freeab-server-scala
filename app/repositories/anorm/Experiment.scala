package repositories.anorm

import java.util.UUID
import anorm.SqlParser._
import anorm._
import models.{FormExperiment}
import play.api.db.DB
import play.api.Play.current

class Experiment(dbConfigName: String, variationRepository: Variation) {

  val experimentParser: RowParser[models.Experiment] = {
    str("id") ~
      str("name") ~
      double("scope") map {
      case id ~ name ~ scope => models.Experiment(id, name, scope)
    }
  }

  def add(formExperiment: FormExperiment): Option[models.Experiment] = DB.withConnection(dbConfigName) { implicit connection =>
    val experimentId = UUID.randomUUID().toString()
    val sql = SQL(
      """INSERT INTO experiments (id, name, scope) VALUES ({experimentId}, {name}, {scope})""")
      .on(
        "experimentId" -> experimentId,
        "name" -> formExperiment.name,
        "scope" -> formExperiment.scope
      )
    val result = sql.executeInsert() // TODO: We should handle errors/exceptions right here

    result match {
      case Some(id) => {
        // TODO: In which case would result be None?
        for (formVariation <- formExperiment.formVariations) {
          variationRepository.add(formVariation, id.toString)
        }
        Option[models.Experiment](models.Experiment(experimentId, formExperiment.name, formExperiment.scope))
      }
      case None => None
    }

  }
}