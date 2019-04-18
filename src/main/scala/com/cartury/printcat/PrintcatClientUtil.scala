package com.cartury.printcat

import java.io.{DataInputStream, DataOutputStream}
import java.net.Socket

case class PrintcatClientUtil(serverHost: String, serverPort: Int) {
  private val GET_PRINTER_LIST = "GetPrinterList"
  private val PRINT = "Print"

  def getPrinterList = action { (_, os) =>
    os writeUTF GET_PRINTER_LIST
  }

  def doPrint(path: String, printer: String) = action { (_, os) =>
    os writeUTF s"$PRINT\t$printer\t$path"
  }

  private def action(body: (DataInputStream, DataOutputStream) => Unit) = {
    val server = new Socket(serverHost, serverPort)
    if (server.isConnected) {
      val os = new DataOutputStream(server.getOutputStream)
      val is = new DataInputStream(server.getInputStream)
      body(is, os)
      val result = is.readUTF
      os.close
      is.close
      result
    } else "printcat服务连接异常"

  }
}