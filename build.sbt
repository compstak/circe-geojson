ThisBuild / scalaVersion := "2.12.8"
ThisBuild / organization := "compstak"

val CirceVersion = "0.12.3"
val ScalaTestVersion = "3.1.0"
val FS2Version = "2.1.0"

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
      "io.circe" %% "circe-parser" % CirceVersion,
      "io.circe" %% "circe-refined" % CirceVersion,
      "org.scalactic" %% "scalactic" % ScalaTestVersion
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3"),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
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
      "org.http4s" %% "http4s-circe" % "0.21.0-M6"
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
      "org.scalacheck" %% "scalacheck" % "1.14.3",
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
      "net.postgis" % "postgis-jdbc" % "2.3.0",
      "org.postgresql" % "postgresql" % "42.2.9"
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
      "org.typelevel" %% "cats-testkit-scalatest" % "1.0.0-RC1",
      "co.fs2" %% "fs2-core" % FS2Version % Test,
      "co.fs2" %% "fs2-io" % FS2Version % Test
    ),
    scalafmtOnCompile := true,
    publish := {}
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
