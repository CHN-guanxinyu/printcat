package com.cartury.printcat.akka

import com.keene.core.implicits._
import com.keene.core.parsers.Arguments

object Main extends App{
  val mainArgs = args.as[ServerArgs]
  mainArgs.mode match {
    case "server" => PrintcatServer main args
    case "client" => PrintcatClient main args
  }
}

class ServerArgs(
  var mode: String = "server"
) extends Arguments {
  override def usage: String =
    """
      |--mode [server|client]
    """.stripMargin
}
