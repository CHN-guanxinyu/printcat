package com.cartury.printcat.nio

import java.io.{DataInputStream, DataOutputStream, File, FileOutputStream}
import java.net.Socket

import com.cartury.printcat.FileUtil

trait Op {
  def action(
    os : DataOutputStream,
    is : DataInputStream
  )

  private def run {
    val server = new Socket("123.206.30.189", 80)
    val os = new DataOutputStream(server.getOutputStream)
    val is = new DataInputStream(server.getInputStream)
    action(os, is)
    os.flush
    os.close
    server.close
  }

  run
}

object Client extends App with Op {
  override def action(
    os : DataOutputStream,
    is : DataInputStream
  ) = {
    os writeUTF "Register\ttest"
    while (true) {
      val fileName = is.readUTF
      val length = is.readLong

      println(fileName, length)
      val file = new File("d:/tmp/tmp.pdf")
      val fos = new FileOutputStream(file)
      val bytes = new Array[Byte](1024)
      var len = is.read(bytes, 0, bytes.length)

      while (len != -1) {
        fos.write(bytes, 0, len)
        fos.flush
        if (len == bytes.length) len = is.read(bytes, 0, bytes.length)
        else len = -1
      }

      fos.close

      FileUtil print file

      file.delete
    }
  }
}

object ListClient extends App with Op {
  override def action(
    os : DataOutputStream,
    is : DataInputStream
  ) {
    os writeUTF "GetClients"
    os.flush

    val result = is.readUTF
    println(result)
  }
}

object PrintClient extends App with Op {
  override def action(
    os : DataOutputStream,
    is : DataInputStream
  ) {
    os writeUTF "Print\ttest"
    os.flush
  }
}