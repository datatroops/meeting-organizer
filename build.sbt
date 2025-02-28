name := """meeting-scheduler"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.16"

libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test
libraryDependencies += "org.mockito" %% "mockito-scala" % "1.17.12" % Test
libraryDependencies += "org.mockito" % "mockito-core" % "3.12.4" % Test
libraryDependencies += "com.typesafe.slick" %% "slick" % "3.4.1"
libraryDependencies += "com.typesafe.play" %% "twirl-api" % "1.6.0"
libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.EarlySemVer

libraryDependencies ++= Seq(
  guice, // Dependency Injection
  ws,
  "org.playframework" %% "play-ws" % "3.0.6",
  "com.typesafe.play" %% "play-slick" % "5.1.0", // Slick integration
  "com.typesafe.play" %% "play-slick-evolutions" % "5.1.0", // DB migrations
  "org.postgresql" % "postgresql" % "42.7.1", // PostgreSQL Driver
  "com.typesafe.play" %% "play-json" % "2.9.4", // JSON handling
  "org.apache.pekko" %% "pekko-actor-typed" % "1.0.1", // WebSockets
  "org.apache.pekko" %% "pekko-stream" % "1.0.1",
  "org.apache.pekko" %% "pekko-slf4j" % "1.0.1",
  "org.mindrot" % "jbcrypt" % "0.4"
)

// Remove these lines as they're causing the version conflict
// libraryDependencies ++= Seq(
//   ws,
//   "com.typesafe.play" %% "play-ws" % "2.8.19"
// )