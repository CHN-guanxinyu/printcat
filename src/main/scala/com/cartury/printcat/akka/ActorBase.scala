package com.cartury.printcat.akka

import akka.actor.ActorSystem
import com.keene.core.implicits._
import com.keene.core.parsers.Arguments
import com.typesafe.config.ConfigFactory

abstract class ActorBase(_system: String) extends App {
  val param = args.as[Args]
  val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=${param.port}").withFallback(ConfigFactory.load)

  implicit val system = ActorSystem(_system, config)
}

case class Args(
  var port: Int = 0, var fileServerHost: String = "localhost", var fileServerPort: Int = 19998,
  var fileServerRoot: String = "/tmp"
) extends Arguments {
  override def usage: String =
    """
      |--port
      |--file-server-host
      |--file-server-port
      |--file-server-root
    """.stripMargin
}