resolvers += Resolver.jcenterRepo

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.25")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.12")
addSbtPlugin("ohnosequences" % "sbt-s3-resolver" % "0.19.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.2.0")
