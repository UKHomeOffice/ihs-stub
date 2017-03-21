name := """ihsstub"""

version := "1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.10.4"

resolvers += "SnakeYAML repository" at "http://oss.sonatype.org/content/groups/public/"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws
) ++ Seq(
  "org.json4s" %% "json4s-native" % "3.2.4",
  "org.json4s" %% "json4s-ext" % "3.2.4",
  "com.nimbusds" % "nimbus-jose-jwt" % "3.6",
  "commons-codec" % "commons-codec" % "1.9",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test"
)

