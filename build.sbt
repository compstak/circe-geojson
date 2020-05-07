lazy val scala212 = "2.12.10"
lazy val scala213 = "2.13.2"
lazy val supportedScalaVersions = List(scala213, scala212)

ThisBuild / scalaVersion := scala212
ThisBuild / organization := "compstak"

val CirceVersion = "0.13.0"
val DisciplineVersion = "1.0.2"
val DisciplineScalatestVersion = "1.0.0"
val FS2Version = "2.2.2"
val ScalaTestVersion = "3.1.0"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
  "-Xfatal-warnings"
)

addCommandAlias("fmtAll", ";scalafmt; test:scalafmt; scalafmtSbt")
addCommandAlias("fmtCheck", ";scalafmtCheck; test:scalafmtCheck; scalafmtSbtCheck")

credentials += Credentials(
  "Sonatype Nexus Repository Manager",
  "nexus.compstak.com",
  sys.env.get("NEXUS_USERNAME").getOrElse(""),
  sys.env.get("NEXUS_PASSWORD").getOrElse("")
)

lazy val commonSettings = Seq(
  organization := "compstak",
  crossScalaVersions := supportedScalaVersions,
  resolvers := Seq("Compstak Maven Group".at("https://nexus.compstak.com/repositories/maven-group")),
  addCompilerPlugin(("org.typelevel" %% "kind-projector" % "0.11.0").cross(CrossVersion.full)),
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
  scalafmtOnCompile := true
)

lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

lazy val publishSettings = Seq(
  publishTo := {
    val prefix = if (isSnapshot.value) "snapshots" else "releases"
    Some(Resolver.url("Sonatype Nexus Repository Manager", url(s"https://nexus.compstak.com/repository/maven-$prefix")))
  },
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ =>
    false
  }
)

lazy val core = (project in file("core"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    name := "circe-geojson-core",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-parser" % CirceVersion,
      "io.circe" %% "circe-refined" % CirceVersion,
      "org.scalactic" %% "scalactic" % ScalaTestVersion % Test
    )
  )

lazy val geoJsonHttp4s = (project in file("geoJsonHttp4s"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    name := "circe-geojson-http4s",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-circe" % "0.21.0"
    )
  )
  .dependsOn(core)

lazy val geoJsonScalaCheck = (project in file("geoJsonScalaCheck"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    name := "circe-geojson-scalacheck",
    libraryDependencies ++= Seq(
      "org.scalacheck" %% "scalacheck" % "1.14.3",
      "io.circe" %% "circe-testing" % CirceVersion
    )
  )
  .dependsOn(core)

lazy val postgis = (project in file("postgis"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    name := "circe-geojson-postgis",
    libraryDependencies ++= Seq(
      "net.postgis" % "postgis-jdbc" % "2.3.0",
      "org.postgresql" % "postgresql" % "42.2.10"
    )
  )
  .dependsOn(core)

lazy val tests = (project in file("tests"))
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(
    name := "circe-geojson-tests",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-jawn" % CirceVersion % Test,
      "io.circe" %% "circe-literal" % CirceVersion % Test,
      "org.scalatest" %% "scalatest" % ScalaTestVersion % Test,
      "org.typelevel" %% "discipline-core" % DisciplineVersion % Test,
      "org.typelevel" %% "discipline-scalatest" % DisciplineScalatestVersion % Test,
      "co.fs2" %% "fs2-core" % FS2Version % Test,
      "co.fs2" %% "fs2-io" % FS2Version % Test
    ),
    publish := {}
  )
  .dependsOn(geoJsonScalaCheck, postgis)

lazy val circeGeoJson = (project in file("."))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(noPublishSettings)
  .settings(name := "circe-geojson")
  .dependsOn(core, postgis, geoJsonScalaCheck, geoJsonHttp4s, tests)
  .aggregate(core, postgis, geoJsonScalaCheck, geoJsonHttp4s, tests)
