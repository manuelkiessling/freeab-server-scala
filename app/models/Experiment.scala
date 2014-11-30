package models

import anorm.SqlParser._
import anorm.{RowParser, SQL, ~}
import play.api.Play.current
import play.api.db.DB

case class Experiment(
  id: Int,
  name: String,
  scope: Float
)

object Experiment {

  val experimentParser: RowParser[Experiment] = {
    int("id") ~
    str("name") ~
    float("scope") map {
      case id ~ name ~ scope => Experiment(id, name, scope)
    }
  }

  def add(formExperiment: FormExperiment): Option[Experiment] = DB.withConnection { implicit connection =>
    val result = SQL(
      """INSERT INTO experiments (name, scope) VALUES ({name}, {scope})""").on(
        "name" -> formExperiment.name,
        "scope" -> formExperiment.scope
      ).executeInsert()

    result match {
      case Some(id) => Option[Experiment](Experiment(id.asInstanceOf[Int], formExperiment.name, formExperiment.scope))
      case None => None
    }

  }

}