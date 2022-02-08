package fr.thekinrar.autopkg

import cats.effect.*
import cats.implicits.*
import fr.thekinrar.autopkg.aur.AUR
import org.http4s.HttpRoutes
import org.http4s.syntax.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.http4s.blaze.client.*
import org.http4s.blaze.server.*
import org.http4s.server.Router

object Main extends IOApp:
  val server =
    for {
      httpClient <- BlazeClientBuilder[IO]
        .resource

      routes = Router(
        "/packages" -> services.packages.routes
      )

      httpServer <- BlazeServerBuilder[IO]
        .bindHttp(8085, "127.0.0.1")
        .withHttpApp(routes.orNotFound)
        .resource
    } yield httpServer

  def run(args: List[String]): IO[ExitCode] =
    server.use(_ => IO.never).as(ExitCode.Success)
