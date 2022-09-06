package fr.thekinrar.autopkg
package discord

import cats.effect.*
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.{EmbedBuilder, JDABuilder, MessageBuilder}

import java.time.ZoneId
import java.time.format.{DateTimeFormatter, FormatStyle}
import java.util.{Date, Locale, Objects}

object Discord {
  def publishResults(results: List[BuildResult]): IO[Unit] = IO {
    val jda = JDABuilder.createLight(sys.env("AUTOPKG_DISCORD_TOKEN")).build
    jda.awaitReady()

    Objects.requireNonNull(jda.getTextChannelById(850413619212058634L))
      .sendMessage(
        if results.size > 25
        then MessageBuilder(buildText(results)).build()
        else MessageBuilder(buildEmbed(results)).build()
      ).queue()

    jda.shutdown()
  }

  def buildEmbed(results: List[BuildResult]): MessageEmbed =
    val b = EmbedBuilder()
      .setTitle("Changelog")
      .setTimestamp(Date().toInstant)
    results.map(resultToField).filter(_.isDefined).map(_.get).foreach(b.addField)
    b.build()

  def buildText(results: List[BuildResult]): String =
    "**Changelog - " +
      DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(Locale.ROOT).withZone(ZoneId.of("Europe/Paris")).format(Date().toInstant) +
      "**\n\n" +
      results.map(resultToString).filter(_.isDefined).map(_.get).mkString("\n")

  def resultToField(result: BuildResult): Option[MessageEmbed.Field] =
    result match {
      case SuccessfulBuild(pkg, prev, next) => Some(MessageEmbed.Field(
        pkg.name, prev match {
          case Some(value) => value + " => " + next
          case None => next
        }, true
      ))
      case FailedBuild(pkg, error) => Some(MessageEmbed.Field(
        pkg.name, "FAILED", true
      ))
      case SkippedBuild(pkg) => None
    }

  def resultToString(result: BuildResult): Option[String] =
    result match {
      case SuccessfulBuild(pkg, prev, next) => Some(
        pkg.name + " " + (prev match {
          case Some(value) => value + " => " + next
          case None => next
        })
      )
      case FailedBuild(pkg, error) => Some(pkg.name + " FAILED")
      case SkippedBuild(pkg) => None
    }
}
