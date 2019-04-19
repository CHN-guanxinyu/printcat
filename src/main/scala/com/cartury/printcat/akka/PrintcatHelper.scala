//package com.cartury.printcat.akka
//
//import java.util.concurrent.atomic.AtomicLong
//import java.util.concurrent.{ConcurrentHashMap, LinkedBlockingQueue}
//
//import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
//import com.cartury.printcat.PrintcatConfig
//
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.Future
//
//object PrintcatHelper {
//  def listPrinter = blockingWithNewRequest(
//    $this ! GetPrinterList(_)
//  ).take
//
//  def print(printer: Long, path: String) = blockingWithNewRequest(
//    $this ! Print(_, printer, path)
//  ).take
//
//  def printcatConfig: PrintcatConfig = ///
//
//  private lazy val system = ActorSystem("Printcat")
//  private lazy val $this = system actorOf Props(new PrintcatHelper(printcatConfig))
//
//  private lazy val printcatServer = system actorSelection
//    s"akka.tcp://Printcat@${printcatConfig.PRINTCAT_SERVER_HOST}:${printcatConfig.PRINTCAT_SERVER_PORT}/user/server"
//
//  private val result = new ConcurrentHashMap[Long, LinkedBlockingQueue[String]]()
//  private val _nextRequestId = new AtomicLong(0)
//  private def nextRequestId = _nextRequestId.getAndIncrement
//
//  private def newRequest = {
//    val rid = nextRequestId
//    result put(rid, new LinkedBlockingQueue(1))
//    rid
//  }
//
//  private def blockingWithNewRequest(f: Long => Unit) = {
//    val reqId = newRequest
//    f(reqId)
//    result.get(reqId)
//  }
//  private def requestCompleted(reqId: Long, resp: String) = result remove reqId put resp
//}
//
//class PrintcatHelper(conf: PrintcatConfig) extends Actor with ActorLogging {
//
//  def listPrinter = PrintcatHelper.blockingWithNewRequest {
//    PrintcatHelper.printcatServer ! GetPrinterList(_)
//  }
//
//  def print(printer: Long, path: String) = PrintcatHelper.blockingWithNewRequest {
//    PrintcatHelper.printcatServer ! Print(_, printer, path)
//  }
//
//  override def receive: Receive = {
//    case GetPrinterList(reqId) => Future{
//      PrintcatHelper.requestCompleted(reqId, listPrinter.take)
//    }
//    case Print(reqId, printerId, relativePath) => Future{
//      PrintcatHelper.requestCompleted(reqId, print(printerId, relativePath).take)
//    }
//
//    case PrinterListResult(reqId, result) =>
//      log info s"printer list result $reqId $result"
//      PrintcatHelper.requestCompleted(reqId, result)
//
//    case PrintSuceessResp(reqId, printer, path) =>
//      log info
//        s"""
//           |Print Succeed:
//           |[
//           |  printer => $printer,
//           |  path => $path
//           |]
//         """.stripMargin
//
//      PrintcatHelper.requestCompleted(reqId, s"成功打印文件: $path")
//
//    case PrintErrorResp(reqId, printer, path, err) =>
//      log info
//        s"""
//           |Print Error:
//           |[
//           |  printer => $printer,
//           |  path => $path,
//           |  error => $err
//           |]
//           """.stripMargin
//      PrintcatHelper.requestCompleted(reqId, s"打印异常:$path, $err")
//  }
//
//
//}
