package com.cartury.printcat.akka.file

import java.io.File
import java.nio.file._
import java.util.UUID

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream._
import akka.stream.scaladsl._
import com.cartury.printcat.akka.{ActorBase, DownloadFile}

import scala.util._

object FileClient extends ActorBase("FileClient") {
  implicit val mat = ActorMaterializer()
  implicit val ec = system.dispatcher
  system.actorOf(Props[FileClient], "client")

  def downloadFileTo(request: HttpRequest, destPath: String, f: File => Unit) = {
    val futResp = Http(system).singleRequest(request)
    futResp.andThen { case Success(r@HttpResponse(StatusCodes.OK, _, entity, _)) => entity.dataBytes.runWith(FileIO.toPath(Paths.get(destPath))).onComplete { case _ => f(new File(destPath)) }
    case Success(r@HttpResponse(code, _, _, _)) => println(s"Download request failed, response code: $code")
      r.discardEntityBytes()
    case Success(_) => println("Unable to download file!")
    case Failure(err) => println(s"Download failed: ${err.getMessage}")
    }

  }
}

class FileClient extends Actor with ActorLogging {
  implicit val ec = context.system.dispatcher

  override def receive: Receive = {
    case DownloadFile(path, f) => val request = HttpRequest(uri = s"http://localhost:19998/file/exchange/" + path)
      val localPath = s"d:/akkadld/${UUID.randomUUID}"
      FileClient.downloadFileTo(request, localPath, f)
  }
}
