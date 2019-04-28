// package com.lc.ibps.platform.rest.printer

// import java.util.concurrent.atomic.AtomicLong
// import java.util.concurrent.{ConcurrentHashMap, LinkedBlockingQueue}

// import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
// import com.cartury.printcat.akka._
// import com.cartury.printcat.{ConfigurationHelper, PrintcatConfig}

// object Helper {
//   //list all printers(block)
//   def listPrinter = blockingWithNewRequest(
//     $this ! GetPrinterList(_)
//   )

//   //submit print job(block)
//   def print(printer: Long, path: String) = blockingWithNewRequest(
//     $this ! Print(_, printer, path)
//   )

//   private val printcatConfig =
//       ConfigurationHelper load getClass.getClassLoader.getResourceAsStream("conf/prop/printcat.properties")
// //      ConfigurationHelper load "D:\\Document\\ws\\ibps-app\\src\\main\\resources\\conf\\prop\\printcat.properties"
//   private val system = ActorSystem("Printcat")

//   private val $this = system actorOf Props(new Helper(printcatConfig))

//   private val printcatServer = system actorSelection
//     s"akka.tcp://Printcat@${printcatConfig.PRINTCAT_SERVER_HOST}:${printcatConfig.PRINTCAT_SERVER_PORT}/user/server"

//   private val result = new ConcurrentHashMap[Long, LinkedBlockingQueue[String]]()
//   private val _nextRequestId = new AtomicLong(0)
//   private def nextRequestId = _nextRequestId.getAndIncrement

//   private def newRequest = {
//     val rid = nextRequestId
//     result put(rid, new LinkedBlockingQueue(1))
//     rid
//   }

//   private def withNewRequest(f: Long => Unit) ={
//     val reqId = newRequest
//     f(reqId)
//     result get reqId
//   }
//   private def blockingWithNewRequest = withNewRequest(_: Long => Unit) take

//   private def requestCompleted(reqId: Long, resp: String) = result remove reqId put resp
// }

// class Helper(conf: PrintcatConfig) extends Actor with ActorLogging {

//   override def receive: Receive = {
//     case PrinterListResult(reqId, result) =>
//       log info s"printer list result $reqId $result"
//       Helper.requestCompleted(reqId, result)

//     case PrintSuceessResp(reqId, printer, path) =>
//       println(
//         s"""
//           |Print Succeed:
//           |[
//           |  printer => $printer,
//           |  path => $path
//           |]
//         """.stripMargin
//       )
//       Helper.requestCompleted(reqId, s"成功打印文件: $path")

//     case PrintErrorResp(reqId, printer, path, err) =>
//       println(
//         s"""
//           |Print Error:
//           |[
//           |  printer => $printer,
//           |  path => $path,
//           |  error => $err
//           |]
//           """.stripMargin
//       )

//       Helper.requestCompleted(reqId, s"打印异常:$path, $err")

//     case m => Helper.printcatServer ! m
//   }


// }
