name := "nxmlreader"

version := "0.1-SNAPSHOT"

organization := "ai.lum"

scalaVersion := "2.11.8"

scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation")

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % "1.0.5"
)


// Publishing settings

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
  <url>https://github.com/lum-ai/nxmlreader</url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      </license>
    </licenses>
    <scm>
      <url>scm:git:github.com/lum-ai/nxmlreader</url>
      <connection>scm:git:git@github.com:lum-ai/nxmlreader.git</connection>
    </scm>
    <developers>
      <developer>
        <id>lum-ai-dev-role</id>
        <name>lum.ai</name>
        <url>lum.ai</url>
      </developer>
    </developers>
  )