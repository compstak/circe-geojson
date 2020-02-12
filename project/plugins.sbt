resolvers += Resolver.jcenterRepo

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.6.0")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.13")
addSbtPlugin("ohnosequences" % "sbt-s3-resolver" % "0.19.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.3.0")
