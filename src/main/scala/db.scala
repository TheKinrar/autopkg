package fr.thekinrar.autopkg

import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*

import sys.env

val xa = Transactor.fromDriverManager[IO](
  "org.postgresql.Driver", sys.env("AUTOPKG_DB_URI"), sys.env("AUTOPKG_DB_USER"), sys.env("AUTOPKG_DB_PASSWORD")
)

case class Package(name: String)
case class Build(pkg: String, id: Long, version: String, success: Boolean)

object pkg {
  def findByName(name: String): IO[Option[Package]] =
    sql"select * from packages where name=$name"
      .query[Package].option.transact(xa)

  def findAll(): IO[List[Package]] =
    sql"select * from packages"
      .query[Package].to[List].transact(xa)

  def insert(name: String): IO[Unit] =
    sql"insert into packages (name) values ($name)"
      .update.run.transact(xa).map(_ => ())
}

object builds {
  def findLatest(pkg: String): IO[Option[Build]] =
    sql"select * from builds where package=$pkg order by id desc limit 1"
      .query[Build].option.transact(xa)

  def findLatestSuccessful(pkg: String): IO[Option[Build]] =
    sql"select * from builds where package=$pkg AND success=true order by id desc limit 1"
      .query[Build].option.transact(xa)

  def insert(id: Long, pkg: String, version: String, success: Boolean): IO[Unit] =
    sql"insert into builds (id, package, version, success) values ($id, $pkg, $version, $success)"
      .update.run.transact(xa).map(_ => ())
}