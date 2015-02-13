package models

import java.util.UUID
import anorm.SqlParser._
import anorm.{RowParser, SQL, ~}
import play.api.Play.current
import play.api.db.DB

case class Experiment(
  id: String,
  name: String,
  scope: Double
)
{
  lazy val variations: List[Variation] = DB.withConnection { implicit connection =>
    SQL(
      """SELECT * FROM variations WHERE experimentId = {experimentId}""")
      .on("experimentId" -> id)
      .as(Variation.variationParser *)
  }
}

object Experiment {

  val experimentParser: RowParser[Experiment] = {
    str("id") ~
    str("name") ~
    double("scope") map {
      case id ~ name ~ scope => Experiment(id, name, scope)
    }
  }

  def add(formExperiment: FormExperiment)(implicit dbConfig: DbConfig): Option[Experiment] = DB.withConnection(dbConfig.dbName) { implicit connection =>
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
      case Some(id) => { // TODO: In which case would result be None?
        for (formVariation <- formExperiment.formVariations) {
          Variation.add(formVariation, id.toString)
        }
        Option[Experiment](Experiment(experimentId, formExperiment.name, formExperiment.scope))
      }
      case None => None
    }

  }

}