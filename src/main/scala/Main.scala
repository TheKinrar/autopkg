package fr.thekinrar.autopkg

import cats.effect.*
import cats.implicits.*
import org.http4s.HttpRoutes
import org.http4s.syntax.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.http4s.blaze.server.*

object Main extends IOApp:
  val routes = services.packages.routes

  def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(8085, "127.0.0.1")
      .withHttpApp(routes.orNotFound)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
