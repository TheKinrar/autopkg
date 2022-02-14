package fr.thekinrar.autopkg

import aur.AurClient
import aur.PackageInfo
import services.{AurService, PackagesService}

import cats.data.*
import cats.effect.*
import cats.implicits.*
import cats.syntax.*
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router

class BuildTask(aurClient: AurClient) {
  def run(): IO[Unit] =
    for {
      packages <- pkg.findAll()
      packagesWithInfo <- withInfo(packages)
      results <- packagesWithInfo.traverse(processPackage)
      _ <- results.traverse(IO.println)
    } yield IO.unit

  def withInfo(l: List[Package]): IO[List[(Package, Option[PackageInfo])]] =
    l.traverse(withInfo)

  def withInfo(pkg: Package): IO[(Package, Option[PackageInfo])] =
    for {
      info <- aurClient.info(pkg.name)
    } yield (pkg, info)

  def processPackage(pkgWithInfo: (Package, Option[PackageInfo])): IO[BuildResult] =
    val pkg = pkgWithInfo._1

    if pkgWithInfo._2.isEmpty then IO(FailedBuild(pkg, "no pkg info"))
    else
      val info = pkgWithInfo._2.get

      IO(SuccessfulBuild(pkg))

abstract class BuildResult {
  def pkg: Package
}
case class SuccessfulBuild(pkg: Package) extends BuildResult
case class FailedBuild(pkg: Package, error: String) extends BuildResult