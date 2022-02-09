package fr.thekinrar.autopkg
package aur

import cats.effect.*
import io.circe.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.client.*
import org.http4s.client.middleware.Logger
import org.http4s.syntax.literals.uri

import scala.util.{Failure, Success, Try}

class AurClient(client: Client[IO]) {
  def info(name: String): IO[Option[PackageInfo]] =
    val req = Request[IO](
      method = Method.GET,
      uri = uri"https://aur.archlinux.org/rpc"
        .withQueryParam("v", "5")
        .withQueryParam("type", "info")
        .withQueryParam("arg", name)
    )

    client.expect[AurResponse](req)
      .map(r => if r.resultCount == 0 then None else Some(r.results.head))

  def pkgbuild(name: String): IO[String] =
    val req = Request[IO](
      method = Method.GET,
      uri = uri"https://aur.archlinux.org/cgit/aur.git/plain/PKGBUILD"
        .withQueryParam("h", name)
    )

    client.expect[String](req)
}
