package fr.thekinrar.autopkg
package aur

case class PackageInfo(
  name: String,
  baseName: String,

  version: String,

  snapshotURL: String,

  depends: Vector[String],
  makeDepends: Vector[String],
)
