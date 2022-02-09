package fr.thekinrar.autopkg
package aur

import io.circe.{Decoder, Encoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import cats.effect.IO
import org.http4s.{EntityDecoder, EntityEncoder}

case class PackageInfo(
  name: String,
  baseName: String,

  version: String,

  snapshotURL: String,

  depends: Vector[String],
  makeDepends: Vector[String],
)

implicit val dPackageInfo: Decoder[PackageInfo] =
  Decoder.forProduct6("Name", "PackageBase", "Version", "URLPath", "Depends", "MakeDepends")(PackageInfo.apply)
implicit val edPackageInfo: EntityDecoder[IO, PackageInfo] = jsonOf[IO, PackageInfo]
implicit val ePackageInfo: Encoder[PackageInfo] =
  Encoder.forProduct6("Name", "PackageBase", "Version", "URLPath", "Depends", "MakeDepends")(p => (p.name, p.baseName, p.version, p.snapshotURL, p.depends, p.makeDepends))
implicit val eePackageInfo: EntityEncoder[IO, PackageInfo] = jsonEncoderOf[IO, PackageInfo]
