ThisBuild / scalaVersion := "2.12.8"
ThisBuild / organization := "compstak"

val CirceVersion = "0.11.1"
val ScalaTestVersion = "3.0.8"
val FS2Version = "1.0.4"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
  "-Ypartial-unification",
  "-Xfatal-warnings"
)

addCommandAlias("fmtAll", ";scalafmt; test:scalafmt; scalafmtSbt")
addCommandAlias("fmtCheck", ";scalafmtCheck; test:scalafmtCheck; scalafmtSbtCheck")

lazy val core = (project in file("core"))
  .settings(
    name := "circe-geojson-core",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-java8" % CirceVersion,
      "io.circe" %% "circe-parser" % CirceVersion,
      "io.circe" %% "circe-refined" % CirceVersion,
      "org.scalactic" %% "scalactic" % ScalaTestVersion
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.6"),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.0"),
    scalafmtOnCompile := true,
    publishTo := {
      val prefix = if (isSnapshot.value) "snapshots" else "releases"
      Some(s3resolver.value("CompStak", s3(s"compstak-maven/$prefix")))
    },
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { _ =>
      false
    }
  )

lazy val geoJsonHttp4s = (project in file("geoJsonHttp4s"))
  .settings(
    name := "circe-geojson-http4s",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-circe" % "0.20.6"
    ),
    scalafmtOnCompile := true,
    publishTo := {
      val prefix = if (isSnapshot.value) "snapshots" else "releases"
      Some(s3resolver.value("CompStak", s3(s"compstak-maven/$prefix")))
    },
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { _ =>
      false
    }
  )
  .dependsOn(core)

lazy val geoJsonScalaCheck = (project in file("geoJsonScalaCheck"))
  .settings(
    name := "circe-geojson-scalacheck",
    libraryDependencies ++= Seq(
      "org.scalacheck" %% "scalacheck" % "1.13.5",
      "io.circe" %% "circe-testing" % CirceVersion
    ),
    scalafmtOnCompile := true,
    publishTo := {
      val prefix = if (isSnapshot.value) "snapshots" else "releases"
      Some(s3resolver.value("CompStak", s3(s"compstak-maven/$prefix")))
    },
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { _ =>
      false
    }
  )
  .dependsOn(core)

lazy val postgis = (project in file("postgis"))
  .settings(
    name := "circe-geojson-postgis",
    libraryDependencies ++= Seq(
      "org.postgis" % "postgis-jdbc" % "1.3.3"
    ),
    scalafmtOnCompile := true,
    publishTo := {
      val prefix = if (isSnapshot.value) "snapshots" else "releases"
      Some(s3resolver.value("CompStak", s3(s"compstak-maven/$prefix")))
    },
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { _ =>
      false
    }
  )
  .dependsOn(core)

lazy val tests = (project in file("tests"))
  .settings(
    name := "circe-geojson-tests",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-jawn" % CirceVersion % Test,
      "io.circe" %% "circe-literal" % CirceVersion % Test,
      "org.scalatest" %% "scalatest" % ScalaTestVersion % Test,
      "org.typelevel" %% "cats-testkit" % "1.6.1" % Test,
      "co.fs2" %% "fs2-core" % FS2Version % Test,
      "co.fs2" %% "fs2-io" % FS2Version % Test
    ),
    scalafmtOnCompile := true,
    publishTo := {
      None
    },
    publishArtifact := false
  )
  .dependsOn(geoJsonScalaCheck, postgis)

lazy val circeGeoJson = (project in file("."))
  .settings(
    name := "circe-geojson",
    publishTo := {
      val prefix = if (isSnapshot.value) "snapshots" else "releases"
      Some(s3resolver.value("CompStak", s3(s"compstak-maven/$prefix")))
    },
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { _ =>
      false
    }
  )
  .dependsOn(core, postgis, geoJsonScalaCheck, geoJsonHttp4s, tests)
  .aggregate(core, postgis, geoJsonScalaCheck, geoJsonHttp4s, tests)
