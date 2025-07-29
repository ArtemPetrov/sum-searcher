scalaVersion := "2.13.7"

name := "sum-searcher"
organization := "sum.searcher"
version := "1.0"

val AkkaHttpVersion = "10.5.2"
val AkkaVersion = "2.8.4"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.5.18",
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % AkkaHttpVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.19" % Test,
  "org.mockito" % "mockito-core" % "5.18.0" % Test
)

scalacOptions += "-Xlint"

assembly / mainClass := Some("sum.searcher.App")
assembly / assemblyJarName := "sum-searcher.jar"
assembly / assemblyMergeStrategy := {
  case PathList("module-info.class") => MergeStrategy.discard
  case x => (assembly / assemblyMergeStrategy).value(x)
}
