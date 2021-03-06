import Dependencies._
import scala.util.Properties._

ThisBuild / scalaVersion := "2.13.4"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.zhpooer"
ThisBuild / organizationName := "zhpooer"
ThisBuild / useCoursier := false
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

bloopExportJarClassifiers in Global := Some(Set("sources"))

addCommandAlias("build", "prepare; test")
addCommandAlias("prepare", "fix; fmt")
addCommandAlias("check", "fixCheck; fmtCheck")
addCommandAlias("fix", "all compile:scalafix test:scalafix")
addCommandAlias(
  "fixCheck",
  "compile:scalafix --check; test:scalafix --check"
)
addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias(
  "fmtCheck",
  "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck"
)

val http4sVersion = "0.21.13"
val tapirVersion  = "0.17.0"
val circeVersion  = "0.13.0"

lazy val root = (project in file("."))
  .settings(
    name := "zio-dojo",
    libraryDependencies += scalaTest % Test,
    libraryDependencies ++= Seq(
      "org.postgresql"               % "postgresql"               % "42.2.16",
      "org.typelevel"               %% "cats-core"                % "2.2.0",
      "org.typelevel"               %% "cats-effect"              % "2.2.0",
      "is.cir"                      %% "ciris"                    % "1.2.1",
      "io.circe"                    %% "circe-core"               % circeVersion,
      "io.circe"                    %% "circe-generic"            % circeVersion,
      "io.circe"                    %% "circe-parser"             % circeVersion,
      "com.beachape"                %% "enumeratum"               % "1.6.1",
      "com.beachape"                %% "enumeratum-circe"         % "1.6.1",
      "org.tpolecat"                %% "doobie-hikari"            % "0.9.0",
      "org.tpolecat"                %% "doobie-postgres"          % "0.9.0",
      "org.tpolecat"                %% "doobie-quill"             % "0.9.0",
      "org.tpolecat"                %% "doobie-core"              % "0.9.0",
      "org.http4s"                  %% "http4s-core"              % http4sVersion,
      "org.http4s"                  %% "http4s-dsl"               % http4sVersion,
      "org.http4s"                  %% "http4s-blaze-server"      % http4sVersion,
      "org.http4s"                  %% "http4s-blaze-client"      % http4sVersion,
      "dev.zio"                     %% "zio"                      % "1.0.3",
      "dev.zio"                     %% "zio-macros"               % "1.0.3",
      "dev.zio"                     %% "zio-interop-cats"         % "2.2.0.1",
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-http4s"  % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs"       % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-zio"                % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-core"               % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe"         % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"      % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-zio-http4s-server"  % tapirVersion,
      "dev.zio"                     %% "zio-logging"              % "0.5.4",
      "dev.zio"                     %% "zio-logging-slf4j"        % "0.5.4",
      "ch.qos.logback"               % "logback-classic"          % "1.2.3"  % Runtime,
      "org.specs2"                  %% "specs2-core"              % "4.10.0" % Test
    )
  )
scalafixDependencies in ThisBuild += "com.nequissimus" %% "sort-imports" % "0.5.5"
addCompilerPlugin("com.olegpy"     %% "better-monadic-for" % "0.3.1")
addCompilerPlugin(("org.typelevel" %% "kind-projector"     % "0.11.1").cross(CrossVersion.full))

scalacOptions in Test ++= Seq("-Yrangepos")
scalacOptions += "-Ymacro-annotations"
// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.

enablePlugins(FlywayPlugin)

flywayUrl := envOrElse("DB_URL", "jdbc:postgresql://db:5432/hellodb")
flywayUser := envOrElse("DB_USER", "postgres")
flywayPassword := envOrElse("DB_PASSWORD", "postgres")
flywayLocations += "filesystem:db/migration"
