package fr.thekinrar.autopkg
package repo

import cats.data.*
import cats.effect.*
import cats.implicits.*
import cats.syntax.*

import java.io.File
import scala.language.postfixOps
import scala.sys.process.*

object Repo {
  def add(db: File, pkg: File): IO[Unit] = IO {
    "repo-add -R " + db.getAbsolutePath + " " + pkg.getAbsolutePath !!
  }
}
