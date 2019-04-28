package com.cartury.printcat.akka

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import com.cartury.printcat.ConfigurationHelper

import scala.concurrent.Await
import scala.concurrent.duration._
object Helper {
  implicit val timeout = Timeout(5 seconds)
  def main(args: Array[String]): Unit = {
    println(print(0, "/file/2019/04/f1d157605a7611e909d2ef931dc538ba.pdf"))
    system.terminate
  }
  //list all printers(block)
  def listPrinter = await(GetPrinterList) match {
    case PrinterListResult(result) => result
    case _ => "error"
  }

  //submit print job(block)
  def print(printerId: Long, path: String) = await(Print(printerId, path)) match{
    case PrintSuceessResp => "success"
    case PrintErrorResp(err) => err
  }

  private val printcatConfig =
//    ConfigurationHelper load getClass.getClassLoader.getResourceAsStream("conf/prop/printcat.properties")
        ConfigurationHelper load "D:\\Document\\ws\\printcat\\conf\\local-server.properties"
  private val system = ActorSystem("Printcat")

  private val printcatServer = system actorSelection
    s"akka.tcp://Printcat@${printcatConfig.PRINTCAT_SERVER_HOST}:${printcatConfig.PRINTCAT_SERVER_PORT}/user/server"

  private def await(msg: Any) =
    Await.result(printcatServer ? msg, 10 seconds)
}
