ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.6"

val awsSdkVersion = "2.20.160"
val sttpVersion = "3.9.0"
val circeVersion = "0.14.13"

lazy val root = (project in file("."))
  .settings(
    name := "data-collector",
    libraryDependencies ++= Seq(
      "software.amazon.awssdk" % "dynamodb" % awsSdkVersion,

      "com.softwaremill.sttp.client3" %% "core" % sttpVersion,
      "com.softwaremill.sttp.client3" %% "circe" % sttpVersion,

      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,

      "com.typesafe" % "config" % "1.4.2",

      "org.slf4j" % "slf4j-api" % "2.0.7",
      "ch.qos.logback" % "logback-classic" % "1.4.8",
    )
  )
