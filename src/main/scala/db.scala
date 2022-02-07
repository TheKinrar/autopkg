package fr.thekinrar.autopkg

import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*

val xa = Transactor.fromDriverManager[IO](
  "org.postgresql.Driver", "jdbc:postgresql://localhost/autopkg", "autopkg", "autopkg"
)

case class Package(id: Long, name: String)
case class Build(id: Long, pkg: String, version: String)

object pkg {
  def findByName(name: String): IO[Option[Package]] =
    sql"select * from packages where name=$name"
      .query[Package].option.transact(xa)

  def findAll(): IO[List[Package]] =
    sql"select * from packages"
      .query[Package].to[List].transact(xa)
}