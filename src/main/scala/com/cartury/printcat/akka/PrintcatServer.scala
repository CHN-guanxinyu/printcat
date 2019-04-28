package com.cartury.printcat.akka

import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{ConcurrentHashMap, Executors, TimeUnit}

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.cartury.printcat.PrintcatConfig
import com.cartury.printcat.akka.file.FileServer

import scala.collection.JavaConversions._
import scala.concurrent.Future

object PrintcatServer extends ActorBase("Printcat") {
  system.actorOf(Props(new PrintcatServer(printcatConfig)), "server")
  implicit val ec = system.dispatcher
  Future { FileServer main args }
  override def sysConfigStr: String =
    s"""
       |akka.remote.netty.tcp.hostname=${printcatConfig.PRINTCAT_SERVER_HOST}
       |akka.remote.netty.tcp.port=${printcatConfig.PRINTCAT_SERVER_PORT}
     """.stripMargin
}

class PrintcatServer(conf: PrintcatConfig) extends Actor with ActorLogging {
  private val debug = false

  override def receive = {
    case Register(name) => processRegister(name)

    case GetPrinterList(reqId) => processListPrinters(reqId)

    case Print(reqId, printerId, relativePath) => processPrint(reqId, printerId, relativePath)

    case PrintSuceess(jobId) => processPrintSuccess(jobId)

    case PrintError(jobId, err) => processPrintError(jobId, err)

    case HeartBeat(id) => processHeartBeat(id)

    case Close => context.system.terminate
  }

  private val _nextClientId = new AtomicLong(0)
  private def nextClientId = _nextClientId.getAndIncrement

  private val _nextJobId = new AtomicLong(0)
  private def nextJobId = _nextJobId.getAndIncrement
  //jobId -> (printerId, filePath, requestId, userCaller)
  private val _printJobList = new ConcurrentHashMap[Long, (Long, String, Long, ActorRef)]()

  private val _printerEndpoint = new ConcurrentHashMap[Long, (String, ActorRef)]()
  private val _printerDeadList = new ConcurrentHashMap[Long, (String, ActorRef)]()
  private val _heartBeatInfo = new ConcurrentHashMap[Long, Long]()
  private val _printerMaybeDead = new ConcurrentHashMap[Long, Long]()

  private val pool = Executors.newScheduledThreadPool(2)

  override def preStart() = {
    checkPrinterHeartBeat
    checkPrinterMaybeDeat
  }

  private def checkPrinterHeartBeat() = pool.scheduleWithFixedDelay(new Runnable {
    override def run() = {
      logDebug(
        s"""|===================================================
            |curTime -> ${format(curTime)}
            |_printerEndpoint -> ${_printerEndpoint}
            |_printerDeadList -> ${_printerDeadList}
            |_heartBeatInfo -> ${_heartBeatInfo.mapValues(format)}
            |_printerMaybeDead -> ${_printerMaybeDead.mapValues(format)}
          """.stripMargin)
      _heartBeatInfo.foreach { case (id, lastTime) => if (curTime - lastTime > 3000) {
        log info s"[client-$id] 短时间超时，加入低级队列"
        _heartBeatInfo remove id
        _printerMaybeDead.put(id, curTime)
        _printerDeadList.put(id, _printerEndpoint remove id)
      }
      }
    }
  }, 0, 3, TimeUnit.SECONDS)

  private def checkPrinterMaybeDeat() = pool.scheduleWithFixedDelay(new Runnable {
    override def run() = {
      log info "检测低级心跳队列"
      _printerMaybeDead.toMap.foreach { case (id, lastTime) => log info s"${curTime - lastTime}"
        if (curTime - lastTime > 10000) {
          log info s"[client-$id] 长时间超时，移除"
          _printerMaybeDead remove id
          _printerDeadList remove id
        }
      }
    }
  }, 0, 10, TimeUnit.SECONDS)

  private def curTime = System.currentTimeMillis

  private def logDebug(str: String) = if (debug) println(str)


  private def processRegister(printer: String): Unit ={
    log info "register"
    val cid = nextClientId
    _printerEndpoint.put(cid, (printer, sender))
    _heartBeatInfo.put(cid, curTime)
    sender ! RegisterResp(cid)
  }
  private def processListPrinters(reqId: Long)={
    val printersJson = "{" + _printerEndpoint.mapValues(_._1)
      .map{ case (id, name) =>
        s"'$id':'$name'"
      }.mkString(",") + "}"

    log info s"GetPrinterList $printersJson"

    val msg = PrinterListResult(reqId, printersJson)
    sender ! msg
  }
  private def processPrint(reqId: Long, printerId: Long, path: String) ={
    log info s"print $printerId $path"
    if (_printerEndpoint containsKey printerId) {
      val jobId = nextJobId
      _printJobList.put(jobId, (printerId, path, reqId, sender))
      val printer = _printerEndpoint.get(printerId)._2
      printer ! DoPrint(jobId, path)
    } else sender ! PrintErrorResp(reqId, "", path, "打印节点网络异常")
  }
  private def getJobCaller(id: Long) =
    _printJobList remove id

  private def processPrintSuccess(jobId: Long) =
    getJobCaller(jobId) match {
      case (id, path, reqId, caller) =>
        caller ! PrintSuceessResp(reqId, _printerEndpoint get id _1, path)
    }

  private def processPrintError(jobId: Long, err: String) =
    getJobCaller(jobId) match {
      case (id, path, reqId, caller) =>
        caller ! PrintErrorResp(reqId, _printerEndpoint get id _1, path, err)
    }

  private def processHeartBeat(id: Long)={
    log info s"recieved heartbeat from client-$id"
    val h = _heartBeatInfo
    val d = _printerMaybeDead
    if ((h containsKey id) || (d containsKey id)) {
      h put(id, curTime)
      if (d containsKey id) {
        _printerEndpoint put(id, _printerDeadList remove id)
        _printerMaybeDead remove id
      }
    } else {
      log info s"[client-$id] 过期，重新注册"
      sender ! OutOfDateBeat
    }
  }
  private def format(time: Long): Unit ={
    new SimpleDateFormat("HH:mm:ss").format(new Date(time))
  }

}