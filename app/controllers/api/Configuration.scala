package controllers.api

import models.DbConfig

trait Configuration {

  // Implicit database configuration for API models
  implicit val dbConfig = DbConfig("api")
  
}
