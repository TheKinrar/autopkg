package fr.thekinrar.autopkg

import aur.AurClient
import aur.PackageInfo
import services.{AurService, PackagesService}

import cats.data.*
import cats.effect.*
import cats.implicits.*
import cats.syntax.*
import fr.thekinrar.autopkg.docker.DockerClient
import org.apache.commons.io.FileUtils
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router

import java.io.File
import java.net.URL
import java.nio.file.Files

class BuildTask(aurClient: AurClient, dockerClient: DockerClient) {
  def run(): IO[Unit] =
    for {
      packages <- pkg.findAll()
      packagesWithInfo <- packages.traverse(withInfo)
      results <- packagesWithInfo.traverse(processPackage)
      _ <- results.traverse(IO.println)
    } yield IO.unit

  def withInfo(pkg: Package): IO[(Package, Option[PackageInfo])] =
    for {
      info <- aurClient.info(pkg.name)
    } yield (pkg, info)

  def processPackage(pkgWithInfo: (Package, Option[PackageInfo])): IO[BuildResult] =
    val pkg = pkgWithInfo._1

    if pkgWithInfo._2.isEmpty then IO(FailedBuild(pkg, "no pkg info"))
    else
      val info = pkgWithInfo._2.get

      builds.findLatestSuccessful(pkg.id)
        .flatMap {
          case Some(latest) =>
            if latest.version != info.version
            then buildPackage(pkg, info)
            else IO(SkippedBuild(pkg))
          case None => buildPackage(pkg, info)
        }

  def buildPackage(pkg: Package, info: PackageInfo): IO[BuildResult] =
    def postBuild(exitCode: Int): IO[BuildResult] =
      if exitCode == 0 then IO(SuccessfulBuild(pkg))
      else IO(FailedBuild(pkg, "Exit code " + exitCode))

    for {
      tmpDir <- makeTempDir(pkg.name)
      _ <- copyURLToFile("https://aur.archlinux.org" + info.snapshotURL, File(tmpDir, "src.tar.gz"))
      container <- dockerClient.createContainer(tmpDir.getAbsolutePath)
      _ <- container.start()
      exitCode <- container.waitFor()
      _ <- deleteDirectory(tmpDir)
      res <- postBuild(exitCode)
    } yield res

  // There's probably a better way of doing this
  def makeTempDir(name: String): IO[File] = IO {
    val f = File(sys.env("AUTOPKG_TMP_DIR"), name)
    if(f.isDirectory) FileUtils.deleteDirectory(f)
    f.mkdirs()
    f
  }

  def deleteDirectory(dir: File): IO[Unit] = IO {
    FileUtils.deleteDirectory(dir)
  }

  def copyURLToFile(url: String, file: File): IO[Unit] = IO {
    FileUtils.copyURLToFile(URL(url), file, 1000, 1000)
  }
}

abstract class BuildResult {
  def pkg: Package
}
case class SuccessfulBuild(pkg: Package) extends BuildResult
case class SkippedBuild(pkg: Package) extends BuildResult
case class FailedBuild(pkg: Package, error: String) extends BuildResult
