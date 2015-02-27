package repositories.anorm

import java.sql.SQLException
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

  // TODO: Use Either or Try instead of a tuple!
  def add(formExperiment: FormExperiment): (Option[models.Experiment], Option[String]) = DB.withConnection(dbConfigName) { implicit connection =>
    val experimentId = UUID.randomUUID().toString()
    val sql = SQL(
      """INSERT INTO experiments (id, name, scope) VALUES ({experimentId}, {name}, {scope})""")
      .on(
        "experimentId" -> experimentId,
        "name" -> formExperiment.name,
        "scope" -> formExperiment.scope
      )
    try {
      val result = sql.executeInsert()
      result match {
        case Some(id) => { // TODO: In which case would result be None?
          for (formVariation <- formExperiment.formVariations) {
            try {
              variationRepository.add(formVariation, id.toString)
            } catch {
              // Spring JDBC kapselt alle DB Typen
              case e: SQLException => {
                SQL(
                  """DELETE FROM experiments WHERE id = {experimentId}""")
                  .on(
                    "experimentId" -> experimentId
                  )
                return (None, Some("Error while storing the experiment's variations"))
              }
            }
          }
          (Option[models.Experiment](models.Experiment(experimentId, formExperiment.name, formExperiment.scope)), None)
        }
        case None => (None, Some("An error occured"))
      }
    } catch {
      case e: SQLException => {
        return (None, Some("An experiment with this name already exists"))
      }
    }
  }
}