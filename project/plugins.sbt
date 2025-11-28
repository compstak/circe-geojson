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

addSbtPlugin("com.github.sbt" % "sbt-git" % "2.1.0")
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.4.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.3")
addSbtPlugin("com.compstak" % "sbt-maven-publisher" % "0.1.1")
addSbtPlugin("com.github.cb372" % "sbt-explicit-dependencies" % "0.3.1")
