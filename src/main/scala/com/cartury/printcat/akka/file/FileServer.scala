package com.cartury.printcat.akka.file

import java.io.File
import java.nio.file._

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpEntity._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream._
import akka.stream.scaladsl._
import com.cartury.printcat.akka.{ActorBase, PrintError}

object FileServer extends ActorBase("PrintcatFileSystem") {
  implicit val httpMat = ActorMaterializer()
  implicit val httpEC = system.dispatcher

  val printcatServer = system actorSelection
    s"akka.tcp://Printcat@${printcatConfig.PRINTCAT_SERVER_HOST}:${printcatConfig.PRINTCAT_SERVER_PORT}/user/server"

  def fileStream(filePath: String, chunkSize: Int) = limitableByteSource(FileIO.fromPath(Paths get filePath, chunkSize))

  val route = pathPrefix("file") {
    (get & path("exchange" / Remaining)) { resource =>
      println(resource split "0xfff" toList)
      val Array(fp, jobId) = resource split "0xfff"
      val path = printcatConfig.FILE_SERVER_ROOT + "/" + fp
      if (new File(path).exists) withoutSizeLimit {
        val stream = fileStream(path, 256)
        complete(HttpEntity(ContentTypes.`application/octet-stream`, stream))
      } else {
        printcatServer ! PrintError(jobId.toLong, s"file not exist: $fp")
        failWith(new NoSuchFileException(fp))
      }
    }
  }

  Http().bindAndHandle(route, printcatConfig.FILE_SERVER_HOST, printcatConfig.FILE_SERVER_PORT.toInt)

  override def sysConfigStr = ""
}