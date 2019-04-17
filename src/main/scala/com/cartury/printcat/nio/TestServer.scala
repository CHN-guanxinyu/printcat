package com.cartury.printcat.nio

import java.io.{DataInputStream, DataOutputStream}
import java.net.{ServerSocket, Socket}

object TestServer extends App {
  val listener = new ServerSocket(8080)
  while (true) {
    val client = listener.accept
    println(client)
    val os = new DataOutputStream(client.getOutputStream)
    val os1 = new DataOutputStream(client.getOutputStream)
    println(client.getOutputStream.equals(client.getOutputStream))
  }
}

object TestClient extends App {
  val server = new Socket("localhost", 8080)
  println(new DataInputStream(server.getInputStream).readUTF)
}
