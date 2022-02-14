package fr.thekinrar.autopkg

import aur.AurClient
import services.{AurService, PackagesService}

import cats.effect.*
import cats.implicits.*
import org.http4s.HttpRoutes
import org.http4s.blaze.client.*
import org.http4s.blaze.server.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.http4s.server.Router
import org.http4s.syntax.*
import scopt.OParser

object Main extends IOApp:
  def server =
    for {
      httpClient <- BlazeClientBuilder[IO]
        .resource

      routes = Router(
        "/aur" -> new AurService(new AurClient(httpClient)).routes,
        "/packages" -> new PackagesService().routes,
      )

      httpServer <- BlazeServerBuilder[IO]
        .bindHttp(8085, "127.0.0.1")
        .withHttpApp(routes.orNotFound)
        .resource
    } yield httpServer

  def build =
    for {
      httpClient <- BlazeClientBuilder[IO]
        .resource
    } yield new BuildTask(new AurClient(httpClient))

  val cliBuilder = OParser.builder[CliConfig]
  val cliParser = {
    import cliBuilder.*

    OParser.sequence(
      programName("autopkg"),
      cmd("web")
        .action((_, c) => c.copy(action = "web"))
        .text("Start HTTP server"),
      cmd("build")
        .action((_, c) => c.copy(action = "build"))
        .text("Build out-of-date packages"),
      checkConfig { c =>
        if c.action.nonEmpty then success else failure("Please specify a command.")
      }
    )
  }

  def run(args: List[String]): IO[ExitCode] =
    OParser.parse(cliParser, args, CliConfig()) match {
      case Some(config) => config.action match {
        case "web" => server.use(_ => IO.never).as(ExitCode.Success)
        case "build" => build.use(t => t.run()).as(ExitCode.Success)
        case _ => IO.pure(ExitCode.Error)
      }
      case None => IO.pure(ExitCode.Error)
    }

case class CliConfig(
  action: String = "",
)
