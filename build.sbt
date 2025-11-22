ThisBuild / scalaVersion := "2.13.17"
// TODO: scala 3.5 was chosen among others because:
// TODO: scala 3.6, 3.7 cannot work with endpoints4s
ThisBuild / crossScalaVersions := Seq("2.13.17", "3.5.2")

inThisBuild(
  List(
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
      ),
      Developer(
        "dmitry.worker",
        "Dmitrii Voronov",
        "dmitry.worker@gmail.com",
        url("https://github.com/dmitry-worker")
      )
    )
  )
)

val CirceVersion = "0.14.4"
val DisciplineVersion = "1.5.1"
val DisciplineScalatestVersion = "2.2.0"
val FS2Version = "3.6.1"
val ScalaTestVersion = "3.2.12"

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
  scalafmtOnCompile := true
)

lazy val noPublishSettings = Seq(
  publish / skip := true
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
      "org.http4s" %% "http4s-circe" % "0.23.5"
    )
  )
  .dependsOn(core)

lazy val geoJsonScalaCheck = (project in file("geoJsonScalaCheck"))
  .settings(commonSettings)
  .settings(
    name := "circe-geojson-scalacheck",
    libraryDependencies ++= Seq(
      "org.scalacheck" %% "scalacheck" % "1.19.0",
      "io.circe" %% "circe-testing" % CirceVersion
    )
  )
  .dependsOn(core)

lazy val postgis = (project in file("postgis"))
  .settings(commonSettings)
  .settings(
    name := "circe-geojson-postgis",
    libraryDependencies ++= Seq(
      "net.postgis" % "postgis-jdbc" % "2025.1.1",
      "org.postgresql" % "postgresql" % "42.2.10"
    )
  )
  .dependsOn(core)

lazy val geoJsonEndpoints4s = (project in file("geoJsonEndpoints4s"))
  .settings(commonSettings)
  .settings(
    name := "endpoints4s-geojson-schemas",
    libraryDependencies ++= Seq(
      "org.endpoints4s" %% "algebra" % "1.12.1"
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
