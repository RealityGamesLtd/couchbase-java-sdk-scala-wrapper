import sbt.Keys._

lazy val root = (project in file(".")).settings(
  scalaVersion := "2.12.0",
//  crossScalaVersions := Seq("2.11.8", "2.12.0"),
  organization := "com.realitygames",
  name := "couchbase-java-sdk-scala-wrapper",
  version := "0.1-SNAPSHOT",

  libraryDependencies ++= Seq(
    "com.couchbase.client" % "java-client" % "2.3.5",

    "com.github.nscala-time" %% "nscala-time" % "2.14.0",

    "org.slf4j" % "slf4j-api" % "1.7.21",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
    "ch.qos.logback" % "logback-classic" % "1.1.7" % "test",
    "com.typesafe" % "config" % "1.3.1",

    "org.mdedetrich" %% "scala-json-ast" % "1.0.0-M4",

    "io.circe" %% "circe-core" % "0.6.0" % "provided",
    "io.circe" %% "circe-generic" % "0.6.0" % "provided",
    "io.circe" %% "circe-parser" % "0.6.0" % "provided",

    "org.scalatest" %% "scalatest" % "3.0.0" % "test",

    "io.circe" %% "circe-core" % "0.6.0" % "test",
    "io.circe" %% "circe-generic" % "0.6.0" % "test",
    "io.circe" %% "circe-parser" % "0.6.0" % "test"
  ),

  addCompilerPlugin("org.psywerx.hairyfotr" %% "linter" % "0.1.16")
)
