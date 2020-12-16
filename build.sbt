import Dependencies._

ThisBuild / scalaVersion     := "2.13.3"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.zhpooer"
ThisBuild / organizationName := "zhpooer"

bloopExportJarClassifiers in Global := Some(Set("sources"))

val http4sVersion = "0.21.7"
lazy val root = (project in file("."))
  .settings(
    name := "zio-dojo",
    libraryDependencies += scalaTest % Test,
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.1.0",
      "is.cir" %% "ciris" % "1.2.1",
      "io.circe" %% "circe-core" % "0.13.0",
      "io.circe" %% "circe-generic" % "0.13.0",
      "io.circe" %% "circe-parser" % "0.13.0",
      "com.beachape" %% "enumeratum" % "1.6.1",
      "com.beachape" %% "enumeratum-circe" % "1.6.1",
      "org.tpolecat" %% "doobie-hikari"    % "0.9.0",
      "org.tpolecat" %% "doobie-postgres"  % "0.9.0",
      "org.tpolecat" %% "doobie-quill"     % "0.9.0",
      "org.tpolecat" %% "doobie-core"      % "0.9.0",
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % http4sVersion,
      "dev.zio" %% "zio" % "1.0.3",
      "dev.zio" %% "zio-interop-cats" % "2.2.0.1",
      "is.cir" %% "ciris" % "1.2.1",
//      "com.softwaremill.sttp.tapir" %% "tapir-zio" % "0.17.0-M10",
      "com.softwaremill.sttp.tapir" %% "tapir-core" % "0.16.16",
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % "0.16.16",
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % "0.16.16",
      //      "com.softwaremill.sttp.tapir" %% "tapir-zio-http4s-server" % "0.17.0-M10",
      "org.specs2" %% "specs2-core" % "4.10.0" % Test
    )
  )

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)

scalacOptions in Test ++= Seq("-Yrangepos")
// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
