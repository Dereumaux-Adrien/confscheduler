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
  "postgresql" % "postgresql" % "9.1-901-1.jdbc4"
)
