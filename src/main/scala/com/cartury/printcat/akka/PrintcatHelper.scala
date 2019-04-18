package com.cartury.printcat.akka

import akka.actor.{Actor, ActorLogging, Props}

object PrintcatHelper extends ActorBase("Printcat") {
  system.actorOf(Props[PrintcatHelper])
}

class PrintcatHelper extends Actor with ActorLogging {
  val server = context actorSelection "akka.tcp://Printcat@localhost:10001/user/server"
  //  server ! GetPrinterList
  server ! Print(7, "1.pdf")

  override def receive: Receive = {
    case PrinterListResult(result) => println(result.mkString("\t"))
  }
}
