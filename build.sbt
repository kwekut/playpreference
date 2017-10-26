name := """pref"""

lazy val commonSettings = Seq(
  scalaVersion := "2.11.7",
  version := "1.0-SNAPSHOT"
)

lazy val root = (project in file(".")).
  enablePlugins(PlayScala).
  aggregate(macro).
  dependsOn(macro).
  settings(commonSettings: _*).
  settings(
libraryDependencies ++= Seq(
  jdbc, ws, filters,
  "com.mohiva" %% "play-silhouette" % "4.0.0",
  "com.mohiva" %% "play-silhouette-password-bcrypt" % "4.0.0",
  "com.mohiva" %% "play-silhouette-persistence" % "4.0.0",
  "com.mohiva" %% "play-silhouette-crypto-jca" % "4.0.0",
  "net.codingwell" %% "scala-guice" % "4.0.1",
  "com.iheart" %% "ficus" % "1.2.6",
  "com.typesafe.play" %% "play-mailer" % "5.0.0",
  "ai.x" %% "play-json-extensions" % "0.8.0",
  "org.elasticsearch" % "elasticsearch" % "2.3.3",
  "org.apache.kafka" % "kafka-clients" % "0.9.0.0",
  "com.datastax.cassandra" % "cassandra-driver-core" % "2.2.0-rc3",
  "org.xerial.snappy" % "snappy-java" % "1.0.5",
  "ai.x" %% "play-json-extensions" % "0.8.0",
  "org.abstractj.kalium" % "kalium" % "0.6.0",
  "com.typesafe.akka" % "akka-testkit_2.11" % "2.3.9",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
))
lazy val scalaReflect = Def.setting { "org.scala-lang" % "scala-reflect" % scalaVersion.value }
lazy val macro = (project in file("macro")).
  settings(commonSettings: _*).
  settings(
    libraryDependencies += scalaReflect.value
  )
resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
resolvers += Resolver.jcenterRepo

routesGenerator := InjectedRoutesGenerator

scalacOptions += "-feature"
