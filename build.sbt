name := """play-react"""
organization := "com.example"

version := "1.0-SNAPSHOT"

//This was for the silautaeion
resolvers += "Atlassian Releases" at "https://maven.atlassian.com/public/"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
//libraryDependencies += filters
"org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
"org.typelevel" %% "cats" % "0.9.0",
"com.adrianhurt" %% "play-bootstrap" % "1.0-P25-B3",
"com.h2database" % "h2" % "1.4.194",
"com.typesafe.play" %% "play-slick" % "2.1.0",
"com.typesafe.play" %% "play-slick-evolutions" % "2.1.0",
"com.mohiva" %% "play-silhouette" % "4.0.0",
"com.mohiva" %% "play-silhouette-password-bcrypt" % "4.0.0",
"com.mohiva" %% "play-silhouette-crypto-jca" % "4.0.0",
"com.mohiva" %% "play-silhouette-persistence" % "4.0.0",
"com.mohiva" %% "play-silhouette-testkit" % "4.0.0" % "test",
"com.mohiva" %% "play-silhouette-cas" % "4.0.0",
"com.mohiva" %% "play-silhouette-persistence-reactivemongo" % "4.0.1"
)
// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.example.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.example.binders._"
