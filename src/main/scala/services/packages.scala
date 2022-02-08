package fr.thekinrar.autopkg
package services

import cats.effect.IO
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.syntax._
import org.http4s.dsl._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.blaze.server.*

object packages {
  def routes = HttpRoutes.of[IO] {
    case GET -> Root => Ok(pkg.findAll())
    case GET -> Root / name => pkg.findByName(name).flatMap {
      case Some(pkg) => Ok(pkg)
      case None => NotFound()
    }
  }
}
