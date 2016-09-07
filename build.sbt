import sbt.Keys._

lazy val root = (project in file(".")).settings(
  scalaVersion := "2.11.8",
  organization := "com.realitygames",
  name := "couchbase-java-sdk-scala-wrapper",
  version := "0.1-SNAPSHOT",

  libraryDependencies ++= Seq(
    "com.couchbase.client" % "java-client" % "2.3.3"
  )
)
