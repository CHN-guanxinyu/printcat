package com.cartury.printcat

import java.io.{DataInputStream, DataOutputStream, File, FileInputStream}
import java.net.{ServerSocket, Socket}
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{ConcurrentHashMap, LinkedBlockingQueue, TimeUnit}

import com.keene.core.implicits._
import com.keene.core.parsers.Arguments

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.util.Try

abstract case class Mission(
  var _id : Long,
  var _meta : String = ""
) {
  private val res = new LinkedBlockingQueue[Any](1)

  def putResult(r : Any) = res put r

  def takeResult = res.take
}

case class PrintMission(
  _pid : Long,
  paths : String
) extends Mission(_pid, paths)

case class GetNetworkStatusMission(
  _gid : Long
) extends Mission(_gid)

object PrintcatServer extends App {
  val ERROR = 0
  val SUCCESS = 1

  val _port = args.as[ServerArgs].port
  val _listener : ServerSocket = new ServerSocket(_port)
  println(s"start up printcat server on port $_port")

  //all missions
  val _missions = new ConcurrentHashMap[Long, Mission]
  //记录所有任务 printer -> blocking queue of missions(mission id, status)
  val _missionsForPrinter = new ConcurrentHashMap[String, LinkedBlockingQueue[Mission]]

  def allocateMissionsForPrinter(name : String) = {
    val q = new LinkedBlockingQueue[Mission]
    _missionsForPrinter.put(name, q)
    q
  }

  def removePrinter(name : String) = _missionsForPrinter remove name

  def pendingMission(
    name : String,
    mission : Mission
  ) = {
    val id = mission._id
    _missions.put(id, mission)
    _missionsForPrinter get name put mission
    id
  }

  def putResult(
    id : Long,
    res : Any = ""
  ) =
    _missions.get(id) putResult res

  val _nextMissionId = new AtomicLong(0)

  def nextMissionId = _nextMissionId.getAndIncrement

  def recieve(client : Socket) {
    val out = getOutputStream(client)
    val in = getInputStream(client)
    var keepAlive = false
    val msg = in.readUTF

    println(msg)

    val messageArray = msg split "\t"
    val res = messageArray match {
      case Array("Register", name) =>
        processNewClient(name, client)
        keepAlive = true
        SUCCESS

      case Array("GetPrinterList") =>
        val printersAvaliable = processListPrinter(client)
        out writeUTF printersAvaliable.mkString("\t")
        SUCCESS

      case Array("Print", printer, paths) =>
        processPrint(printer, paths)
        SUCCESS

      case _ =>
        ERROR
    }

    out writeUTF res.toString
    out.flush

    if (!keepAlive) {
      in.close
      out.close
      client.close
    }
  }

  /**
    * 根据printer name和对应的socket, 轮询任务队列, 根据不同任务进行不同处理逻辑
    *
    * @param printer
    * @param client
    * @return
    */
  def processNewClient(
    printer : String,
    client : Socket
  ) = Future {
    val missions = allocateMissionsForPrinter(printer)

    while (!connectionLost(client)) {
      //每15min没有新任务, 判断一次connection lost
      val mission = missions.poll(15, TimeUnit.MINUTES)

      if (mission != null) mission match {
        case PrintMission(id, paths) =>
          val files = paths split "," map (new File(_))
          sendFiles(client, files)
        case GetNetworkStatusMission(id) =>
          val isLost = connectionLost(client)
          putResult(id, isLost)
      }
    }
    client.close
    removePrinter(printer)
    println(s"lost connection error: $client")
  }


  def getOutputStream(client : Socket) = new DataOutputStream(client getOutputStream)

  def getInputStream(client : Socket) = new DataInputStream(client getInputStream)

  def sendFiles(
    client : Socket,
    files : Array[File]
  ) = {
    if (connectionLost(client)) {
      println(s"lost connection error: $client")
      ERROR
    } else {
      println(s"send ${files.length} files to client [$client]")
      val os = getOutputStream(client)

      os writeInt files.length
      os.flush
      SUCCESS
    }
    //      (files map sendFile(os)).sum - files.length
  }


  def sendFile(os : DataOutputStream)
    (file : File) = if (file.exists) {
    os writeUTF file.getName
    os.flush
    os writeLong file.length
    os.flush

    writeFileObject(os, file)
  } else {
    println("file not exist!", file)
    ERROR
  }

  def writeFileObject(
    os : DataOutputStream,
    file : File
  ) : Int = {
    val bytes = new Array[Byte](1024)
    val fis = new FileInputStream(file)
    var length = fis.read(bytes, 0, bytes.length)
    while (length != -1) {
      os.write(bytes, 0, length)
      os.flush
      length = fis.read(bytes, 0, bytes.length)
    }
    SUCCESS
  }

  def connectionLost(socket : Socket) = Try(socket.sendUrgentData(0xFF)).isFailure


  def processListPrinter(client : Socket) = {
    //提交GetNetworkStatusMission, 得到任务id
    val printerToMissionId = _missionsForPrinter.keys.toList
      .map { printer =>
        printer -> pendingMission(printer, GetNetworkStatusMission(nextMissionId))
      }

    printerToMissionId.flatMap { case (printer, mid) =>
      val lost = _missions(mid).takeResult.asInstanceOf[Boolean]
      if (lost) {
        client.close
        removePrinter(printer)
        None
      } else Some(printer)
    }
  }

  def processPrint(
    printer : String,
    paths : String
  ) = {
    val id = pendingMission(printer, PrintMission(nextMissionId, paths))
    _missions(id).takeResult.asInstanceOf[String]
  }

  while (true) recieve {
    _listener.accept
  }
}

case class ServerArgs(
  var port : Int = 80
) extends Arguments {
  override def usage : String =
    """
      |--port
    """.stripMargin
}
