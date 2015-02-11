package controllers.api

import models.DbConfig

trait Configuration {

  // Implicit database configuration for models
  implicit val dbConfig = DbConfig("default")
  
}
