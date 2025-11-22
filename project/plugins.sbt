resolvers ++= Seq(
  "Compstak Backend"
    .at("https://compstak-prod-278696104475.d.codeartifact.us-east-1.amazonaws.com/maven/backend/")
)
credentials += Credentials(
  "compstak-prod/backend",
  "compstak-prod-278696104475.d.codeartifact.us-east-1.amazonaws.com",
  "aws",
  sys.env.getOrElse("CODEARTIFACT_AUTH_TOKEN", "")
)

addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.16")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.6")
addSbtPlugin("com.tapad" % "sbt-docker-compose" % "1.0.35")
addSbtPlugin("au.com.onegeek" %% "sbt-dotenv" % "2.1.233")
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.1.0")
addSbtPlugin("com.github.sbt" % "sbt-git" % "2.0.1")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.1.1")
addSbtPlugin("com.compstak" % "sbt-maven-publisher" % "0.1.1")
