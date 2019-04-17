package com.cartury.printcat.nio

import java.io._
import java.net.{ServerSocket, Socket}

import scala.collection.mutable

object Server extends App {
  val file = new File(args(0))

  val listener : ServerSocket = new ServerSocket(args(1).toInt)

  val clients = mutable.Map.empty[String, Socket]
  while (true) {
    val client = listener.accept
    val out = getOutputStream(client)
    val in = getInputStream(client)
    var keepAlive = false
    val messageArray = in.readUTF split "\t"
    println(messageArray.mkString(" "))
    messageArray match {
      case Array("Register", name) =>
        clients put(name, client)
        keepAlive = true
      case Array("GetClients") =>
        out writeUTF clients.keys.mkString("\t")
        out.flush
      case Array("Print", name) =>
        val os = getOutputStream(clients(name))
        sendFile(os)
      case _ =>
        "error"
    }

    if (!keepAlive) {
      in.close
      out.close
    }
  }

  def getOutputStream(client : Socket) = new DataOutputStream(client getOutputStream)

  def getInputStream(client : Socket) = new DataInputStream(client getInputStream)

  def sendFile(os : DataOutputStream) : Unit = {
    os writeUTF file.getName
    os.flush
    os writeLong file.length
    os.flush

    writeFileObject(os)
  }

  def writeFileObject(os : DataOutputStream) : Unit = {
    val bytes = new Array[Byte](1024)
    val fis = new FileInputStream(file)
    var length = fis.read(bytes, 0, bytes.length)
    while (length != -1) {
      os.write(bytes, 0, length)
      os.flush
      length = fis.read(bytes, 0, bytes.length)
    }
  }
}
