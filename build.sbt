name := """confscheduler"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "com.github.nscala-time" %% "nscala-time" % "1.2.0",
  "com.lambdaworks" % "scrypt" % "1.4.0",
  "postgresql" % "postgresql" % "9.1-901-1.jdbc4",
  "org.apache.commons" % "commons-email" % "1.3.3",
  "com.sksamuel.scrimage" %% "scrimage-core" % "1.4.1",
  "mysql" % "mysql-connector-java" % "5.1.27",
  "com.github.tototoshi" %% "scala-csv" % "1.2.1"
)