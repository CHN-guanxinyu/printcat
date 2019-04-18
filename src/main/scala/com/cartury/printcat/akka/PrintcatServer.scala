package com.cartury.printcat.akka

import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{ConcurrentHashMap, Executors, TimeUnit}

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.cartury.printcat.akka.file.FileServer

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object PrintcatServer extends ActorBase("Printcat") {
  system.actorOf(Props(new PrintcatServer(param)), "server")
}

class PrintcatServer(param: Args) extends Actor with ActorLogging {
  val debug = false

  override def receive = {
    case Register(name) => log info "register"
      val cid = nextClientId
      _printerEndpoint.put(cid, (name, sender))
      _heartBeatInfo.put(cid, curTime)
      sender ! RegisterResp(cid)
    case GetPrinterList => log info "GetPrinterList"
      sender ! PrinterListResult(_printerEndpoint.mapValues(_._1).toList)
    case Print(printerId, relativePath) => log info s"print $printerId $relativePath"
      if (_printerEndpoint containsKey printerId) {
        val printer = _printerEndpoint.get(printerId)._2
        printer ! DoPrint(relativePath)
      }
    case HeartBeat(id) => log info s"recieved heartbeat from client-$id"
      val h = _heartBeatInfo
      val d = _printerMaybeDead
      if ((h containsKey id) || (d containsKey id)) {
        //        log info s"[client-$id] 更新心跳时间"
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

  val _nextClientId = new AtomicLong(0)

  def nextClientId = _nextClientId.getAndIncrement

  val _printerEndpoint = new ConcurrentHashMap[Long, (String, ActorRef)]()
  val _printerDeadList = new ConcurrentHashMap[Long, (String, ActorRef)]()
  val _heartBeatInfo = new ConcurrentHashMap[Long, Long]()
  val _printerMaybeDead = new ConcurrentHashMap[Long, Long]()

  val pool = Executors.newScheduledThreadPool(2)

  override def preStart() = {
    Future {
      FileServer.main(Array(param.fileServerHost, param.fileServerPort.toString, param.fileServerRoot))
    }
    checkPrinterHeartBeat
    checkPrinterMaybeDeat
  }

  def checkPrinterHeartBeat() = pool.scheduleWithFixedDelay(new Runnable {
    override def run() = {
      logDebug(
        s"""|===================================================|curTime -> ${new SimpleDateFormat("HH:mm:ss").format(new Date(curTime))}|_printerEndpoint -> ${_printerEndpoint}|_printerDeadList -> ${_printerDeadList}|_heartBeatInfo -> ${
          _heartBeatInfo.mapValues(v => new SimpleDateFormat("HH:mm:ss").format(new Date(v)))
        }|_printerMaybeDead -> ${_printerMaybeDead.mapValues(v => new SimpleDateFormat("HH:mm:ss").format(new Date(v)))}
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

  def checkPrinterMaybeDeat() = pool.scheduleWithFixedDelay(new Runnable {
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

  def curTime = System.currentTimeMillis

  def logDebug(str: String) = if (debug) {
    println(str)
  }

}

object PrintcatConst {
  val ERROR = 0
  val SUCCESS = 1
}
