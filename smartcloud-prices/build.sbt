import Dependencies._

lazy val root = (project in file("."))
  .settings(
    name := "smartcloud-prices",
    scalaVersion := "2.13.13",
    scalacOptions ~= (_.filterNot(Set("-Xfatal-warnings"))),
    libraryDependencies ++= Seq(
      L.http4s("ember-server"),
      L.http4s("ember-client"),
      L.http4s("circe"),
      L.http4s("dsl"),
      L.circe,
      L.logback,
      L.pureConfig,
      T.munit,
      T.circeLiteral,
      C.betterMonadicFor,
      C.kindProjector
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    assembly / mainClass := Some("prices.Main"),
    assembly / assemblyJarName := "smartcloud-prices.jar",
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", _*) => MergeStrategy.discard
      case _                        => MergeStrategy.first
    }
  )
