import ReleaseTransformations._

name := "nxmlreader"

organization := "ai.lum"

// scalaVersion := "2.11.11"
scalaVersion := "2.12.4"
crossScalaVersions := Seq("2.11.11", "2.12.4")

scalacOptions ++= Seq(
  "-feature",
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  // "-Ywarn-value-discard",
  "-Ywarn-unused",
  "-encoding", "utf8"
)

libraryDependencies ++= Seq(
  "ai.lum" %% "common" % "0.0.8",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.6"
)

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  ReleaseStep(action = Command.process("publishSigned", _)),
  setNextVersion,
  commitNextVersion,
  ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
  pushChanges
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

pomExtra :=
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
      <id>marcovzla</id>
      <name>Marco Antonio Valenzuela Escárcega</name>
      <url>lum.ai</url>
    </developer>
    <developer>
      <id>ghp</id>
      <name>Gus Hahn-Powell</name>
      <url>lum.ai</url>
    </developer>
  </developers>
