package fr.thekinrar.autopkg
package aur

import cats.effect.*
import cats.implicits.*
import io.circe.*
import io.circe.syntax.*
import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}

case class AurResponse(
  typ: String,
  version: Int,
  resultCount: Int,
  results: Vector[PackageInfo]
)

/*
We cannot use Encoder.forProductN as these do not work for nested case classes (PackageInfo is nested in AurResponse)
See https://github.com/circe/circe/issues/561
*/
implicit val dAurResponse: Decoder[AurResponse] = Decoder.instance { c =>
  (
    c.get[String]("type"),
    c.get[Int]("version"),
    c.get[Int]("resultcount"),
    c.get[Vector[PackageInfo]]("results")
    ).mapN(AurResponse.apply)
}
implicit val edAurResponse: EntityDecoder[IO, AurResponse] = jsonOf[IO, AurResponse]
implicit val eAurResponse: Encoder[AurResponse] = Encoder.instance {
  case AurResponse(typ, version, resultCount, results) => Json.obj(
    "type" -> typ.asJson,
    "version" -> version.asJson,
    "resultcount" -> resultCount.asJson,
    "results" -> results.asJson,
  )
}
implicit val eeAurResponse: EntityEncoder[IO, AurResponse] = jsonEncoderOf[IO, AurResponse]
