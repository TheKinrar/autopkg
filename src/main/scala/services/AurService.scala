package fr.thekinrar.autopkg
package services

import aur.*

import cats.effect.IO
import org.http4s.*
import org.http4s.blaze.server.*
import org.http4s.circe.jsonEncoderOf
import org.http4s.dsl.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.http4s.syntax.*

class AurService(aurClient: AurClient) {
  implicit val encPackageInfo: EntityEncoder[IO, PackageInfo] = jsonEncoderOf[IO, PackageInfo]

  def routes = HttpRoutes.of[IO] {
    case GET -> Root / name => aurClient.info(name).flatMap {
      case Some(value) => Ok(value)
      case None => NotFound()
    }
    case GET -> Root / name / "pkgbuild" => Ok(aurClient.pkgbuild(name))
  }
}
