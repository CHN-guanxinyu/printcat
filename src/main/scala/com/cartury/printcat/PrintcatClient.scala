package com.cartury.printcat

import java.io._
import java.net.Socket
import java.nio.file.{FileSystems, Paths, StandardWatchEventKinds}
import java.util.UUID

import com.keene.core.implicits._
import com.keene.core.parsers.Arguments

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Random, Try}

object PrintcatClient extends App {
  val clientArg = args.as[Args]
  println(clientArg)
  private var server: Try[Socket] = _
  Future {
    val watch = FileSystems.getDefault.newWatchService
    val path = Paths.get(clientArg.closeFlag)
    path.register(watch, StandardWatchEventKinds.ENTRY_CREATE)
    println(s"watch close path: ${clientArg.closeFlag}")
    watch.take
    println("检测到关闭指令")
    System exit 0
  }
  while (true) {
    try {
      server = Try(new Socket(clientArg.serverHost, clientArg.serverPort))
      println(register)
    } catch {
      case e: Throwable => e.printStackTrace
        println("连接已断开，尝试重连...")
    }
  }

  private def register() = if (server.isSuccess) {
    val os = new DataOutputStream(server.get.getOutputStream)
    val is = new DataInputStream(server.get.getInputStream)
    os writeUTF s"Register\t${clientArg.localName}"
    val connResult = is.readInt
    if (connResult == PrintcatConst.SUCCESS) {
      println("连接成功")
      waitForFileRecieve(is)
    } else {
      println("连接异常 error_code = " + connResult)
      System.exit(-1)
    }
    val result = is.readUTF
    os.close
    is.close
    result
  } else "printcat服务连接异常"


  private def waitForFileRecieve(is: DataInputStream) = while (true) {
    println(s"waiting for a new file")
    val bytes = new Array[Byte](1024)
    var len = is.read(bytes, 0, bytes.length)
    if (len == -1) {
      println("失去连接")
      System exit -1
    }
    val file = new File(s"${clientArg.tempDir}/${UUID.randomUUID}")
    val fos = new FileOutputStream(file)

    while (len != -1) {
      fos.write(bytes, 0, len)
      fos.flush
      if (len == bytes.length) len = is.read(bytes, 0, bytes.length) else len = -1
    }
    fos.close
    println(s"recieved a new file") //    FileUtil print file
    //    file.delete
  }

}

case class Args(
  var serverHost: String = "localhost", var serverPort: Int = 80, var tempDir: String = "d:/tmp",
  var closeFlag: String = "d:/tmp/close",
  var localName: String = "print-cat-client-" + Random.nextInt(Integer.MAX_VALUE)
) extends Arguments {
  override def usage: String =
    """
      |--server-host
      |--server-port
      |--temp-dir
      |--local-name
      |--close-flag
    """.stripMargin
}