ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.1.1"

val http4sVersion = "0.23.10"
val dockerVersion = "3.2.13"
val doobieVersion = "1.0.0-RC1"
val circeVersion = "0.14.1"

lazy val root = (project in file("."))
  .settings(
    name := "autopkg",
    idePackagePrefix := Some("fr.thekinrar.autopkg"),
    libraryDependencies ++= Seq(
      "commons-io"             %  "commons-io"                 % "2.11.0",
      "com.github.docker-java" %  "docker-java-core"           % dockerVersion,
      "com.github.docker-java" %  "docker-java-transport-httpclient5" % dockerVersion,
      "com.github.scopt"       %% "scopt"                      % "4.0.1",
      "net.dv8tion"            %  "JDA"                        % "5.0.0-alpha.9",
      "org.typelevel"          %% "cats-effect"                % "3.3.6",
      "org.http4s"             %% "http4s-blaze-server"        % http4sVersion,
      "org.http4s"             %% "http4s-blaze-client"        % http4sVersion,
      "org.http4s"             %% "http4s-circe"               % http4sVersion,
      "org.http4s"             %% "http4s-dsl"                 % http4sVersion,
      "org.slf4j"              %  "slf4j-simple"               % "1.7.36",
      "org.tpolecat"           %% "doobie-core"                % doobieVersion,
      "org.tpolecat"           %% "doobie-postgres"            % doobieVersion,
      "io.circe"               %% "circe-core"                 % circeVersion,
      "io.circe"               %% "circe-generic"              % circeVersion,
    )
  )

ThisBuild / assemblyMergeStrategy := {
  case "module-info.class" => MergeStrategy.discard
  case "META-INF/versions/9/module-info.class" => MergeStrategy.discard
  case x =>
    val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
    oldStrategy(x)
}