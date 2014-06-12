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
  "jp.t2v" %% "play2-auth"      % "0.12.0",
  "jp.t2v" %% "play2-auth-test" % "0.12.0" % "test"
)
