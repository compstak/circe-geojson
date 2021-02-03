lazy val scala212 = "2.12.10"
lazy val scala213 = "2.13.2"
lazy val supportedScalaVersions = List(scala213, scala212)

inThisBuild(
  List(
    scalaVersion := scala213,
    organization := "com.compstak",
    homepage := Some(url("https://github.com/compstak/circe-geojson")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        "LukaJCB",
        "Luka Jacobowitz",
        "luka.jacobowitz@gmail.com",
        url("https://github.com/LukaJCB")
      ),
      Developer(
        "goedelsoup",
        "Cory Parent",
        "goedelsoup@gmail.com",
        url("https://github.com/goedelsoup")
      )
    )
  )
)

val CirceVersion = "0.13.0"
val DisciplineVersion = "1.0.2"
val DisciplineScalatestVersion = "1.0.0"
val FS2Version = "2.2.2"
val ScalaTestVersion = "3.1.0"

ThisBuild / scalacOptions ++= Seq(
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

lazy val commonSettings = Seq(
  crossScalaVersions := supportedScalaVersions,
  addCompilerPlugin(("org.typelevel" %% "kind-projector" % "0.11.0").cross(CrossVersion.full)),
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
  scalafmtOnCompile := true
)

lazy val noPublishSettings = Seq(
  skip in publish := true
)

lazy val core = (project in file("core"))
  .settings(commonSettings)
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
  .settings(
    name := "circe-geojson-http4s",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-circe" % "0.21.18"
    )
  )
  .dependsOn(core)

lazy val geoJsonScalaCheck = (project in file("geoJsonScalaCheck"))
  .settings(commonSettings)
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
  .settings(
    name := "circe-geojson-postgis",
    libraryDependencies ++= Seq(
      "net.postgis" % "postgis-jdbc" % "2.3.0",
      "org.postgresql" % "postgresql" % "42.2.10"
    )
  )
  .dependsOn(core)

lazy val geoJsonEndpoints4s = (project in file("geoJsonEndpoints4s"))
  .settings(commonSettings)
  .settings(
    name := "endpoints4s-geojson-schemas",
    libraryDependencies ++= Seq(
      "org.endpoints4s" %% "algebra" % "1.1.0"
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
    )
  )
  .dependsOn(geoJsonScalaCheck, postgis)

lazy val circeGeoJson = (project in file("."))
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(name := "circe-geojson")
  .dependsOn(core, postgis, geoJsonScalaCheck, geoJsonHttp4s, geoJsonEndpoints4s, tests)
  .aggregate(core, postgis, geoJsonScalaCheck, geoJsonHttp4s, geoJsonEndpoints4s, tests)
