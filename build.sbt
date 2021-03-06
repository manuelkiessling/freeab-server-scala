organization := "net.kiessling.manuel"

name := """freeab-server"""

version := "0.0.1"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.5"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "org.xerial" % "sqlite-jdbc" % "3.8.7"
)

javaOptions in Test += "-Dconfig.file=conf/application.test.conf"
