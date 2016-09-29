import sbt.Keys._

lazy val root = (project in file(".")).settings(
  scalaVersion := "2.11.8",
  organization := "com.realitygames",
  name := "couchbase-java-sdk-scala-wrapper",
  version := "0.1-SNAPSHOT",

  libraryDependencies ++= Seq(
    "com.couchbase.client" % "java-client" % "2.3.3",

    "com.github.nscala-time" %% "nscala-time" % "2.14.0",

    "com.typesafe.play" % "play-json_2.11" % "2.5.8",

    "org.slf4j" % "slf4j-api" % "1.7.21" % "test",
    "ch.qos.logback" % "logback-classic" % "1.1.7" % "test",

    "org.scalactic" %% "scalactic" % "3.0.0",
    "org.scalatest" %% "scalatest" % "3.0.0" % "test"
  ),

  addCompilerPlugin("org.psywerx.hairyfotr" %% "linter" % "0.1.15")
)
