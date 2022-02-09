package fr.thekinrar.autopkg
package aur

import cats.effect.*
import cats.implicits.*
import io.circe.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.CirceEntityDecoder.*
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
}

/*
No need for http4s' Entity(De|En)coders here, we're importing org.http4s.circe.CirceEntityDecoder.* which will create
them automatically for every A with a (De|En)coder[A] available.
*/
implicit val decodePackageInfo: Decoder[PackageInfo] =
  Decoder.forProduct6("Name", "PackageBase", "Version", "URLPath", "Depends", "MakeDepends")(PackageInfo.apply)
implicit val encodePackageInfo: Encoder[PackageInfo] =
  Encoder.forProduct6("Name", "PackageBase", "Version", "URLPath", "Depends", "MakeDepends")(p => (p.name, p.baseName, p.version, p.snapshotURL, p.depends, p.makeDepends))
/*
We cannot use Encoder.forProductN as these do not work for nested case classes (PackageInfo is nested in AurResponse)
See https://github.com/circe/circe/issues/561
*/
implicit val decodeAurResponse: Decoder[AurResponse] = Decoder.instance { c =>
  (
    c.get[String]("type"),
    c.get[Int]("version"),
    c.get[Int]("resultcount"),
    c.get[Vector[PackageInfo]]("results")
  ).mapN(AurResponse.apply)
}
implicit val encodeAurResponse: Encoder[AurResponse] = Encoder.instance {
  case AurResponse(typ, version, resultCount, results) => Json.obj(
    "type" -> typ.asJson,
    "version" -> version.asJson,
    "resultcount" -> resultCount.asJson,
    "results" -> results.asJson,
  )
}

case class AurResponse(
 typ: String,
 version: Int,
 resultCount: Int,
 results: Vector[PackageInfo]
)