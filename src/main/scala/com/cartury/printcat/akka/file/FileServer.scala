package com.cartury.printcat.akka.file

import java.nio.file._

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpEntity._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream._
import akka.stream.scaladsl._

object FileServer extends App {
  val (host, port, root) = (args(0), args(1).toInt, args(2))
  implicit val httpSys = ActorSystem("PrintcatFileSystem")
  implicit val httpMat = ActorMaterializer()
  implicit val httpEC = httpSys.dispatcher

  def fileStream(filePath: String, chunkSize: Int) = limitableByteSource(FileIO.fromPath(Paths get filePath, chunkSize))

  val route = pathPrefix("file") {
    (get & path("exchange" / Remaining)) { fp =>
      withoutSizeLimit {
        complete(HttpEntity(ContentTypes.`application/octet-stream`, fileStream(root + "/" + fp, 256)))
      }
    }
  }

  Http().bindAndHandle(route, host, port)
}