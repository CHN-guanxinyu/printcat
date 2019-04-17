package com.cartury.printcat

import java.io._
import java.net.{ServerSocket, Socket}
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{ConcurrentHashMap, LinkedBlockingQueue, TimeUnit}

import com.keene.core.implicits._
import com.keene.core.parsers.Arguments

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

object PrintcatServer extends App {

  import PrintcatConst._

  /**
    * @param client
    */
  def recieve(client: Socket) {
    val out = getOutputStream(client)
    val in = getInputStream(client)
    var keepAlive = false
    val msg = in.readUTF

    println(
      s"""|=========================================|$msg|-----------------------------------------""".stripMargin)

    val messageArray = msg split "\t"
    val res = messageArray match {
      case Array("Register", name) => processNewClient(name, client)
        keepAlive = true
        out writeInt SUCCESS
      case Array("GetPrinterList") => val printersAvaliable = processListPrinter(client)
        out writeUTF printersAvaliable.mkString("\t")
      case Array("Print", printer, paths) => out writeUTF processPrint(printer, paths)
      case c => out writeUTF s"无效的命令: $msg"
    }

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
  def processNewClient(printer: String, client: Socket) = Future {
    println(s"start process client $printer")
    val missions = initMissionsForPrinter(printer)

    while (!connectionLost(client)) {
      println(s"waiting for new mission for client $printer")
      //每15min没有新任务, 判断一次connection lost
      val mission = missions.poll(15, TimeUnit.MINUTES)
      println(s"recieved new mission $mission for client $printer")

      if (mission != null) mission match {
        case PrintMission(id, path) => val file = new File(path)
          putResult(id, sendFile(client, file))
        case GetNetworkStatusMission(id) => val isLost = connectionLost(client)
          putResult(id, isLost)
      }
    }
    client.close
    removePrinter(printer)
  }


  val _port = args.as[ServerArgs].port
  val _listener: ServerSocket = new ServerSocket(_port)
  println(s"start up printcat server on port ${_port}")

  //all missions
  val _missions = new ConcurrentHashMap[Long, Mission]
  //记录所有任务 printer -> blocking queue of missions(mission id, status)
  val _missionsForPrinter = new ConcurrentHashMap[String, LinkedBlockingQueue[Mission]]

  def initMissionsForPrinter(name: String) = {
    val q = new LinkedBlockingQueue[Mission]
    _missionsForPrinter.put(name, q)
    q
  }

  def removePrinter(name: String) = _missionsForPrinter remove name

  def pendingMission(name: String, mission: Mission) = {
    if (_missionsForPrinter containsKey name) {
      val id = mission.id
      _missions.put(id, mission)
      _missionsForPrinter get name put mission
      id
    } else -1L
  }

  def putResult(id: Long, res: Any = "") = _missions.get(id) putResult res

  val _nextMissionId = new AtomicLong(0)

  def nextMissionId = _nextMissionId.getAndIncrement


  def processListPrinter(client: Socket) = {
    //提交GetNetworkStatusMission, 得到任务id
    val printerToMissionId = _missionsForPrinter.keys.toList.map { printer =>
      printer -> pendingMission(printer, GetNetworkStatusMission(nextMissionId))
      }

    printerToMissionId.flatMap { case (printer, mid) => val lost = _missions(mid).takeResult.asInstanceOf[Boolean]
      if (lost) {
        client.close
        removePrinter(printer)
        None
      } else Some(printer)
    }
  }

  def processPrint(printer: String, paths: String) = {
    val id = pendingMission(printer, PrintMission(nextMissionId, paths))
    if (id == -1L) s"打印节点异常:[$printer]" else _missions(id).takeResult.asInstanceOf[String]
  }

  def sendFile(client: Socket, file: File): String = {
    if (connectionLost(client)) {
      val err = s"lost connection error: $client"
      println(err)
      err
    } else {
      println(s"send file ${file.getName} to client [$client]")
      val os = getOutputStream(client)
      val totalSuccess = sendFile(os, file)
      s"成功打印 $totalSuccess 个文件"
    }
  }

  def sendFile(os: DataOutputStream, file: File): Int = if (file.exists) {
    writeFileObject(os, file)
  } else {
    println("file not exist!", file)
    ERROR
  }

  def writeFileObject(os: DataOutputStream, file: File): Int = {
    val bytes = new Array[Byte](1024)
    val fis = new FileInputStream(file)
    var length = fis.read(bytes, 0, bytes.length)
    while (length != -1) {
      writeBytes(os, bytes, length)
      length = fis.read(bytes, 0, bytes.length)
    }
    fis.close
    SUCCESS
  }

  def connectionLost(socket: Socket) = Try(socket.sendUrgentData(0xFF)).isFailure

  def getOutputStream(client: Socket) = new DataOutputStream(client getOutputStream)

  def getInputStream(client: Socket) = new DataInputStream(client getInputStream)

  def writeBytes(os: DataOutputStream, bytes: Array[Byte], length: Int): Unit = {
    os write(bytes, 0, length)
    os flush
  }

  def writeUTF(os: DataOutputStream, data: String) = {
    os writeUTF data
    os flush
  }

  def writeLong(os: DataOutputStream, data: Long): Unit = {
    os writeLong data
    os flush
  }

  def writeInt(os: DataOutputStream, data: Int): Unit = {
    os writeInt data
    os flush
  }

  while (true) try {
    recieve(_listener.accept)
  } catch {
    case e => e.printStackTrace
  }
}

class Mission(var id: Long) {
  private val res = new LinkedBlockingQueue[Any](1)

  def putResult(r: Any) = res put r

  def takeResult = res.take
}

case class PrintMission(_id: Long, path: String) extends Mission(_id)

case class GetNetworkStatusMission(_id: Long) extends Mission(_id)

object PrintcatConst {
  val ERROR = 0
  val SUCCESS = 1
}

case class ServerArgs(var port: Int = 80) extends Arguments {
  override def usage: String =
    """
      |--port
    """.stripMargin
}
