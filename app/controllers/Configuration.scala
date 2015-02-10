package controllers

import models.DbConfig

trait Configuration {

  // Implicit database configuration for models
  implicit val dbConfig = DbConfig("default")
  
}
