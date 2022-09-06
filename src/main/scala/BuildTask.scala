package fr.thekinrar.autopkg

import aur.{AurClient, PackageInfo}
import discord.Discord
import docker.DockerClient
import repo.Repo
import services.{AurService, PackagesService}

import cats.data.*
import cats.effect.*
import cats.implicits.*
import cats.syntax.*
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
      packages <- sys.env.get("AUTOPKG_ONLY") match {
        case Some(name) => pkg.findByName(name).flatMap {
          case Some(pkg) => IO.pure(List(pkg))
          case None => IO.raiseError(Exception(name + " not found (autopkg_only)"))
        }
        case None => pkg.findAll()
      }
      packagesWithInfo <- packages.traverse(withInfo)
      results <- packagesWithInfo.traverse(processPackage)
      filteredResults = results.filter(!_.isInstanceOf[SkippedBuild])
      _ <- filteredResults.traverse(IO.println)
      _ <- Discord.publishResults(filteredResults)
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

      builds.findLatestSuccessful(pkg.name)
        .flatMap {
          case Some(latest) =>
            if latest.version != info.version
            then buildPackage(pkg, info, Some(latest.version))
            else IO(SkippedBuild(pkg))
          case None => buildPackage(pkg, info, None)
        }

  def buildPackage(pkg: Package, info: PackageInfo, prev: Option[String]): IO[BuildResult] =
    def postBuild(exitCode: Int, tmpDir: File): IO[BuildResult] =
      if exitCode == 0 then installPackages(tmpDir)
      else IO(FailedBuild(pkg, "Exit code " + exitCode))

    def installPackages(tmpDir: File): IO[BuildResult] =
      def installPackage(pkg: File): IO[Unit] =
        val repoPkg = File(sys.env("AUTOPKG_REPO_DIR"), pkg.getName)
        for {
          _ <- copyFile(pkg, repoPkg)
          _ <- Repo.add(File(sys.env("AUTOPKG_REPO_DIR"), "aur.db.tar.gz"), pkg)
        } yield IO.unit

      for {
        files <- listFiles(File(tmpDir, "pkgdest"))
        _ <- files.traverse(installPackage)
      } yield SuccessfulBuild(pkg, prev, info.version)

    def insertBuild(res: BuildResult): IO[Unit] = res match {
      case FailedBuild(pkg, error) =>
        builds.findLatest(pkg.name).flatMap {
          case Some(latest) => builds.insert(latest.id + 1, pkg.name, info.version, false)
          case None => builds.insert(1, pkg.name, info.version, false)
        }
      case SkippedBuild(pkg) => IO.unit
      case SuccessfulBuild(pkg, prev, next) =>
        builds.findLatest(pkg.name).flatMap {
          case Some(latest) => builds.insert(latest.id + 1, pkg.name, info.version, true)
          case None => builds.insert(1, pkg.name, info.version, true)
        }
    }

    for {
      tmpDir <- makeTempDir(pkg.name)
      _ <- copyURLToFile("https://aur.archlinux.org" + info.snapshotURL, File(tmpDir, "src.tar.gz"))
      container <- dockerClient.createContainer(tmpDir.getAbsolutePath)
      _ <- container.start()
      exitCode <- container.waitFor()
      res <- postBuild(exitCode, tmpDir)
      _ <- insertBuild(res)
      _ <- deleteDirectory(tmpDir)
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

  def copyFile(from: File, to: File): IO[Unit] = IO {
    FileUtils.copyFile(from, to)
  }

  def listFiles(dir: File): IO[List[File]] = IO {
    dir.listFiles().toList
  }
}

sealed trait BuildResult {
  def pkg: Package
}
case class SuccessfulBuild(pkg: Package, prev: Option[String], next: String) extends BuildResult
case class SkippedBuild(pkg: Package) extends BuildResult
case class FailedBuild(pkg: Package, error: String) extends BuildResult
