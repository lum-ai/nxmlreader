name := "nxmlreader"

version := "0.1-SNAPSHOT"

organization := "ai.lum"

scalaVersion := "2.11.8"

scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation")

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % "1.0.5"
)
