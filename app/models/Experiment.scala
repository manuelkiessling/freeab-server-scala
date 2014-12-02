package models

import anorm.SqlParser._
import anorm.{RowParser, SQL, ~}
import play.api.Play.current
import play.api.db.DB

case class Experiment(
  id: Int,
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
    int("id") ~
    str("name") ~
    double("scope") map {
      case id ~ name ~ scope => Experiment(id, name, scope)
    }
  }

  def add(formExperiment: FormExperiment): Option[Experiment] = DB.withConnection { implicit connection =>
    val result = SQL(
      """INSERT INTO experiments (name, scope) VALUES ({name}, {scope})""")
      .on(
        "name" -> formExperiment.name,
        "scope" -> formExperiment.scope
      ).executeInsert()

    result match {
      case Some(id) => {
        for (formVariation <- formExperiment.formVariations) {
          SQL(
            """INSERT INTO variations (experiment_id, name, weight) VALUES ({experimentId}, {name}, {weight})""")
            .on(
              "experimentId" -> id.asInstanceOf[Int],
              "name" -> formVariation.name,
              "weight" -> formVariation.weight
            ).executeInsert()
        }
        Option[Experiment](Experiment(id.asInstanceOf[Int], formExperiment.name, formExperiment.scope))
      }
      case None => None
    }

  }

}