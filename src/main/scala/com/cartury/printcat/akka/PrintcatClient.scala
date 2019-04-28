package com.cartury.printcat.akka

import java.util.concurrent.{Executors, TimeUnit}

import akka.actor.{Actor, ActorLogging, Props}
import com.cartury.printcat.akka.file.FileClient
import com.cartury.printcat.{FileUtil, PrintcatConfig}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object PrintcatClient extends ActorBase("Printcat") {

  override def sysConfigStr = ""

  Future { FileClient main args }
  system.actorOf(Props(new PrintcatClient(printcatConfig)), "client")
}

class PrintcatClient(conf: PrintcatConfig) extends Actor with ActorLogging {
  private var _id = -1L
  private val pool = Executors.newScheduledThreadPool(1)
  private val server = context actorSelection
    s"akka.tcp://Printcat@${conf.PRINTCAT_SERVER_HOST}:${conf.PRINTCAT_SERVER_PORT}/user/server"

  private lazy val fileClient = context actorSelection
    s"akka.tcp://FileClient@localhost:${conf.FILE_CLIENT_PORT}/user/client"

  override def preStart() = register


  override def receive: Receive = {
    case RegisterResp(id) => processRegisterResp(id)

    case OutOfDateBeat => register

    case DoPrint(jobId, path) => processPrint(jobId, path)

    case PrintSuceess(jobId) => server ! PrintSuceess(jobId)

    case PrintError(jobId, err) => server ! PrintError(jobId, err)

    case Close => context.system.terminate
  }

  private def register = server ! Register(conf.PRINTCAT_CLIENT_NAME)

  private def processRegisterResp(id: Long) = {
    log info s"register success, myid = $id"
    _id = id
    startHeartBeatScheduler()
  }

  private def startHeartBeatScheduler(): Unit = {
    pool.scheduleWithFixedDelay(new Runnable {
      override def run() = {
        server ! HeartBeat(_id)
      }
    }, 0, 2, TimeUnit.SECONDS)
  }

  private def processPrint(jobId: Long, path: String) = {
    log info "do print"
    fileClient ! DownloadFile(jobId, path, file => {
      FileUtil print file
      file.delete
    })
  }

}
