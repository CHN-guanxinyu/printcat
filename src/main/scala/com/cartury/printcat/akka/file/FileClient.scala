package com.cartury.printcat.akka.file

import java.io.File
import java.nio.file._
import java.util.UUID

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream._
import akka.stream.scaladsl._
import com.cartury.printcat.PrintcatConfig
import com.cartury.printcat.akka._

import scala.concurrent.Await
import scala.concurrent.duration._

case class PrintResult(id: Long, success: Boolean, msg: String = "")

class FileClient(conf: PrintcatConfig) extends PrintcatActor {
  override def receive: Receive = {
    case DownloadFile(path, f) =>
      val request = HttpRequest(
        uri = s"http://${conf.FILE_SERVER_HOST}:${conf.FILE_SERVER_PORT}/file/exchange/$path"
      )
      val localPath = s"${conf.FILE_CLIENT_LOCAL_DIR}/${UUID.randomUUID}"
      downloadFileTo(request, localPath, f) match {
        case HttpResponse(StatusCodes.OK, _, entity, _) =>
          Await.result(entity.dataBytes.runWith(FileIO toPath Paths.get(localPath)), 3 seconds) match {
            case _ =>
              f(new File(localPath))
              sender ! DownloadSuceess
          }
        case oth =>
          val msg = s"Unable to download file! $oth"
          sender ! DownloadError(msg)
          log.info(msg)
      }
  }

  implicit val mat = ActorMaterializer()
  val printcatClient = system.actorSelection("user/client")

  def downloadFileTo(request: HttpRequest, destPath: String, f: File => Unit) = {
    val futResp = Http(context.system).singleRequest(request)

    Await.result(futResp, 5 seconds)
  }
}
