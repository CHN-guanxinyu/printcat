package com.cartury.printcat.akka

import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

import akka.actor.ActorRef
import akka.pattern.ask
import com.cartury.printcat.PrintcatConfig
import com.cartury.printcat.akka.file.FileServer

import scala.collection.JavaConversions._
import scala.concurrent.Await
import scala.concurrent.duration._

class PrintcatServer(conf: PrintcatConfig) extends PrintcatActor {

  override def receive = {
    case Register(name) => processRegister(name)

    case GetPrinterList => processListPrinters

    case Print(printerId, relativePath) => processPrint(printerId, relativePath)

    case HeartBeat(printerId) => processHeartBeat(printerId)

    case GetEnv => sender ! conf

    case Close =>
      println("get a shutting down message.")
      sender ! 0
      system.terminate
  }


  private val debug = false
  private val _nextClientId = new AtomicLong(0)
  private def nextClientId = _nextClientId.getAndIncrement

  private val _printerEndpoint = new ConcurrentHashMap[Long, (String, ActorRef)]
  private val _printerDeadList = new ConcurrentHashMap[Long, (String, ActorRef)]

  private val _heartBeatInfo = new ConcurrentHashMap[Long, Long]
  private val _printerMaybeDead = new ConcurrentHashMap[Long, Long]

  private val scheduler = system.scheduler

  FileServer.initialize(conf)

  scheduler.schedule(0 seconds, 3 seconds) {
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

  scheduler.schedule(0 seconds, 10 seconds) {
    log info "检测低级心跳队列"
    _printerMaybeDead.toMap.foreach { case (id, lastTime) => log info s"${curTime - lastTime}"
      if (curTime - lastTime > 10000) {
        log info s"[client-$id] 长时间超时，移除"
        _printerMaybeDead remove id
        _printerDeadList remove id
      }
    }
  }

  private def curTime = System.currentTimeMillis

  private def logDebug(str: String) = if (debug) println(str)

  private def format(time: Long) =
    new SimpleDateFormat("HH:mm:ss").format(new Date(time))


  private def processRegister(printer: String): Unit = {
    log info "register"
    val cid = nextClientId
    _printerEndpoint.put(cid, (printer, sender))
    _heartBeatInfo.put(cid, curTime)
    sender ! RegisterResp(cid)
  }

  private def processListPrinters = {
    val printersJson =
      s"""|{${
            _printerEndpoint.mapValues(_._1)
              .map { case (id, name) => s"'$id':'$name'" }
              .mkString(",")
          }}""".stripMargin

    log info s"GetPrinterList $printersJson"

    sender ! PrinterListResult(printersJson)
  }

  private def processPrint(printerId: Long, path: String) = {
    log info s"print $printerId $path"
    if (_printerEndpoint containsKey printerId) {
      val printer = _printerEndpoint.get(printerId)._2
      Await.result(printer ? DoPrint(path), 3 seconds) match {
        case PrintSuceess =>
          println("server: success")
          sender ! PrintSuceessResp
        case PrintError(err) => sender ! PrintErrorResp(err)
        case _ => PrintErrorResp("打印节点网络异常")
      }
    }
    else sender ! PrintErrorResp("打印节点网络异常")
  }

  private def processHeartBeat(id: Long) = {
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
}