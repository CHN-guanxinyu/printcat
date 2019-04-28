package com.cartury.printcat.akka

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.cartury.printcat.ConfigurationHelper
import com.keene.core.implicits._
import com.keene.core.parsers.Arguments
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
object Main extends App {

  val mainArgs = args.as[MainArgs]
  require(mainArgs.configFile.nonEmpty, "--config-file must be a valid file path")
  require(mainArgs.action.nonEmpty, "--action must be specific")
  require(mainArgs.mode.nonEmpty, "--mode must be specific")

  val printcatConfig = ConfigurationHelper load mainArgs.configFile
  val config = ConfigFactory.parseString(
    mainArgs.mode match {
      case "server" =>
        s"""
           |akka.remote.netty.tcp.hostname=${printcatConfig.PRINTCAT_SERVER_HOST}
           |akka.remote.netty.tcp.port=${if(mainArgs.action == "start")printcatConfig.PRINTCAT_SERVER_PORT else 0}
        """.stripMargin
      case "client" => ""
    }
  ).withFallback(ConfigFactory.load)

  val system = ActorSystem("Printcat", config)
  implicit val ec = system.dispatcher
  implicit val timeout = Timeout(3 seconds)

  mainArgs.action match {
    case "start" =>
      system.actorOf(Props(s"com.cartury.printcat.akka.Printcat${mainArgs.mode.headToUpper}"
        .as[PrintcatActor](printcatConfig)), mainArgs.mode)
    case "stop" =>
      system.actorSelection( s"akka.tcp://Printcat@$getSocket/user/${mainArgs.mode}" ) ? Close onComplete(_ => system.terminate)
  }

  def getSocket= mainArgs mode match {
    case "server" => s"${printcatConfig.PRINTCAT_SERVER_HOST}:${printcatConfig.PRINTCAT_SERVER_PORT}"
    case "client" => s"localhost:${mainArgs.clientPort}"
  }
}

class MainArgs(
  var configFile: String = "",
  var mode: String = "",
  var action: String = "",
  var clientPort: Int = 0
) extends Arguments {
  override def usage: String =
    """
      |--config-file
      |--mode server|client
      |--action start|stop
      |--client-port
    """.stripMargin
}
