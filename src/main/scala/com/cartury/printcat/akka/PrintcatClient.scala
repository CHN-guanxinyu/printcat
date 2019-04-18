package com.cartury.printcat.akka

import java.util.concurrent.{Executors, TimeUnit}

import akka.actor.{Actor, ActorLogging, Props}
import com.cartury.printcat.akka.file.FileClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object PrintcatClient extends ActorBase("Printcat") {
  system.actorOf(Props[PrintcatClient], "client")
}

class PrintcatClient extends Actor with ActorLogging {
  var _id = -1L
  val pool = Executors.newScheduledThreadPool(1)
  var server = context actorSelection "akka.tcp://Printcat@localhost:10001/user/server"
  lazy val fileClient = context actorSelection "akka.tcp://FileClient@localhost:19999/user/client"

  override def preStart() = {
    Future {
      FileClient.main(Array("--port", "19999"))
    }
    register
  }

  override def receive: Receive = {
    case RegisterResp(id) => log info s"register success, myid = $id"
      _id = id
      startHeartBeatScheduler()
    case OutOfDateBeat => register
    case DoPrint(path) => log info "do print"
      fileClient ! DownloadFile(path, file => {
        println(file.length)
      })
  }

  def register = server ! Register("client1")

  def startHeartBeatScheduler(): Unit = {
    pool.scheduleWithFixedDelay(new Runnable {
      override def run() = {
        server ! HeartBeat(_id)
      }
    }, 0, 2, TimeUnit.SECONDS)
  }
}
