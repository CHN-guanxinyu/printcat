package com.cartury.printcat

import java.io.{DataInputStream, DataOutputStream}
import java.net.Socket

import com.keene.core.implicits._
import com.keene.core.parsers.Arguments

import scala.util.Try

object PrintcatClient extends App {
  val clientArg = args.as[Args]
  private lazy val server = Try(new Socket(clientArg.host, clientArg.port))

  println(register)

  private def register() = action { (
    is,
    os
  ) =>
    os writeUTF s"Register\t${clientArg.localName}"
    waitForFileRecieve(is)
  }

  private def waitForFileRecieve(is : DataInputStream) = while (true) {
    val fileCount = is.readInt
    println(s"print $fileCount files")
    /*for(_ <- 0 until fileCount){
      val fileName = is.readUTF
      val length = is.readLong
      println(fileName, length)

      val file = new File("d:/tmp/" + fileName)
      val fos = new FileOutputStream(file)
      val bytes = new Array[Byte](1024)
      var len = is.read(bytes, 0, bytes.length)

      while(len != -1){
        fos.write(bytes, 0, len)
        fos.flush
        if(len == bytes.length)len = is.read(bytes, 0, bytes.length)
        else len = -1
      }

      fos.close
//      FileUtil print file
//      file.delete
    }*/
  }

  private def action(body : (DataInputStream, DataOutputStream) => Unit) = {
    if (server.isSuccess) {
      val os = new DataOutputStream(server.get.getOutputStream)
      val is = new DataInputStream(server.get.getInputStream)
      body(is, os)
      val result = is.readUTF
      os.close
      is.close
      result
    } else "printcat服务连接异常"

  }
}

class Args(
  var host : String = "localhost",
  var port : Int = 80,
  //  var localName: String = "print-cat-client-" + Random.nextInt(Integer.MAX_VALUE)
  var localName : String = "print-cat-client"
) extends Arguments {
  override def usage : String =
    """
      |--host
      |--port
      |--local-name
    """.stripMargin
}