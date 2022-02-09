package fr.thekinrar.autopkg
package services

import cats.effect.IO
import fr.thekinrar.autopkg.aur.AurClient
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.syntax.*
import org.http4s.dsl.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.http4s.blaze.server.*

class PackagesService {
  def routes = HttpRoutes.of[IO] {
    case GET -> Root => Ok(pkg.findAll())
    case GET -> Root / name => pkg.findByName(name).flatMap {
      case Some(pkg) => Ok(pkg)
      case None => NotFound()
    }
    case PUT -> Root / name => for {
      _ <- pkg.insert(name)
      ret <- NoContent()
    } yield ret
  }
}
