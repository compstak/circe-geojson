

ThisBuild / scalaVersion := "2.12.8"
ThisBuild / organization := "compstak"

val CirceVersion = "0.11.1"
val ScalaTestVersion = "3.0.5"
val FS2Version = "1.0.4"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
  "-Ypartial-unification",
  "-Xfatal-warnings",
)

lazy val lizzones = (project in file("."))
  .settings(
    name := "circe-geojson",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-java8" % CirceVersion,
      "io.circe" %% "circe-parser" % CirceVersion,
      "io.circe" %% "circe-refined" % CirceVersion,
      "io.circe" %% "circe-jawn" % CirceVersion % Test,
      "io.circe" %% "circe-literal" % CirceVersion % Test,
      "org.scalactic" %% "scalactic" % ScalaTestVersion,
      "org.scalatest" %% "scalatest" % ScalaTestVersion % Test,
      "co.fs2" %% "fs2-core" % FS2Version % Test,
      "co.fs2" %% "fs2-io" % FS2Version % Test
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.6"),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.0"),
    publishTo               := {
      val prefix = if (isSnapshot.value) "snapshots" else "releases"
      Some(s3resolver.value("CompStak", s3(s"compstak-maven/$prefix")))
    },
    publishMavenStyle       := true,
    publishArtifact in Test := false,
    pomIncludeRepository    := { _ => false },
  )
