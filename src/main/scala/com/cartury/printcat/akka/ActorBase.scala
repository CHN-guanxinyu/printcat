package com.cartury.printcat.akka

import akka.actor.ActorSystem
import com.cartury.printcat.ConfigurationHelper
import com.keene.core.implicits._
import com.keene.core.parsers.Arguments
import com.typesafe.config.ConfigFactory

abstract class ActorBase(_system: String) extends App {
  val printcatConfig = ConfigurationHelper load args.as[Args].configFile

  def sysConfigStr: String

  val config = ConfigFactory.parseString(sysConfigStr).withFallback(ConfigFactory.load)

  implicit val system = ActorSystem(_system, config)
}

case class Args(var configFile: String = "../conf/default.properties") extends Arguments {
  override def usage: String =
    """
      |--config-file
    """.stripMargin
}