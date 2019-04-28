package com.cartury.printcat.akka

import akka.actor.{ActorRef, Props}
import akka.pattern.ask
import com.cartury.printcat.akka.file.FileClient
import com.cartury.printcat.{FileUtil, PrintcatConfig}

import scala.concurrent.Await
import scala.concurrent.duration._

class PrintcatClient(localEnv: PrintcatConfig) extends PrintcatActor {
  override def receive: Receive = {
    case env: PrintcatConfig =>
      fileClient = context.system.actorOf(Props(new FileClient(env)), "fileClient")
      register

    case RegisterResp(id) => processRegisterResp(id)

    case DoPrint(path) => processPrint(path)

    case OutOfDateBeat => register

    case Close =>
      println("get a shutting down message.")
      sender ! 0
      system.terminate
  }

  private val server = context actorSelection
    s"akka.tcp://Printcat@${localEnv.PRINTCAT_SERVER_HOST}:${localEnv.PRINTCAT_SERVER_PORT}/user/server"

  private var fileClient: ActorRef = _

  private var remoteEnv: PrintcatConfig = _

  override def preStart() = server ! GetEnv

  private def register = server ! Register(localEnv.PRINTCAT_CLIENT_NAME)

  private def processRegisterResp(myid: Long) = {
    log info s"register success, myid = $myid"
    context.system.scheduler.schedule(0 seconds, 2 seconds)(server ! HeartBeat(myid))
  }

  private def processPrint(path: String) = {
    log info "do print"
    Await.result(fileClient ? DownloadFile(path, file => {
      FileUtil print file
      file.delete
    }), 3 seconds) match {
      case DownloadSuceess => sender ! PrintSuceess
      case DownloadError(err) => sender ! PrintError(err)
      case _ => sender ! PrintError("timeout")
    }
  }

}
