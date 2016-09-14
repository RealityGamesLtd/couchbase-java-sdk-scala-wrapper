import sbt.Keys._

lazy val root = (project in file(".")).settings(
  scalaVersion := "2.11.8",
  organization := "com.realitygames",
  name := "couchbase-java-sdk-scala-wrapper",
  version := "0.1-SNAPSHOT",

  libraryDependencies ++= Seq(
    "com.couchbase.client" % "java-client" % "2.3.3",

    "com.github.nscala-time" %% "nscala-time" % "2.14.0",

    "com.typesafe.play" % "play-json_2.11" % "2.5.6",

    "org.scalactic" %% "scalactic" % "3.0.0",
    "org.scalatest" %% "scalatest" % "3.0.0" % "test"
  ),

  addCompilerPlugin("org.psywerx.hairyfotr" %% "linter" % "0.1.15")
)
