package com.cartury.printcat.akka

import akka.actor.{Actor, ActorLogging, Props}

object PrintcatHelper extends ActorBase("Printcat") {
  system.actorOf(Props[PrintcatHelper])
  Thread sleep 1000
  system.terminate

  override def sysConfigStr = ""
}

class PrintcatHelper extends Actor with ActorLogging {
  val server = context actorSelection "akka.tcp://Printcat@localhost:10001/user/server"
  //    server ! GetPrinterList
  server ! Print(0, "2.pdf1")

  override def receive: Receive = {
    case PrinterListResult(result) => println(result.mkString("\t"))
    case PrintSuceessResp(printer, path) => println(
      s"""|Print Succeed: [|  printer => $printer,|  path => $path|]
         """.stripMargin)
    case PrintErrorResp(printer, path, err) => println(
      s"""|Print Error: [|  printer => $printer,|  path => $path,|  error => $err|]
         """.stripMargin)
  }


}
