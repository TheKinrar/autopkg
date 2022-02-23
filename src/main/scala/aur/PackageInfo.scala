package fr.thekinrar.autopkg
package aur

import cats.effect.*
import cats.implicits.*
import io.circe.*
import io.circe.syntax.*
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

// Not using forProductN because we need to handle missing Depends and MakeDepends
implicit val dPackageInfo: Decoder[PackageInfo] = Decoder.instance { c =>
  (
    c.get[String]("Name"),
    c.get[String]("PackageBase"),
    c.get[String]("Version"),
    c.get[String]("URLPath"),
    c.getOrElse[Vector[String]]("Depends")(Vector[String]()),
    c.getOrElse[Vector[String]]("MakeDepends")(Vector[String]())
    ).mapN(PackageInfo.apply)
}
implicit val edPackageInfo: EntityDecoder[IO, PackageInfo] = jsonOf[IO, PackageInfo]
implicit val ePackageInfo: Encoder[PackageInfo] =
  Encoder.forProduct6("Name", "PackageBase", "Version", "URLPath", "Depends", "MakeDepends")(p => (p.name, p.baseName, p.version, p.snapshotURL, p.depends, p.makeDepends))
implicit val eePackageInfo: EntityEncoder[IO, PackageInfo] = jsonEncoderOf[IO, PackageInfo]
