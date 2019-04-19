package com.cartury.printcat.akka.file

import java.io.File
import java.nio.file._
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream._
import akka.stream.scaladsl._
import com.cartury.printcat.PrintcatConfig
import com.cartury.printcat.akka.{ActorBase, DownloadFile, PrintError, PrintSuceess}

import scala.concurrent.Future
import scala.util._

object FileClient extends ActorBase("FileClient") {
  system.actorOf(Props(new FileClient(printcatConfig)), "client")

  override def sysConfigStr =
    s"""|akka.remote.netty.tcp.port=${printcatConfig.FILE_CLIENT_PORT}
    """.stripMargin
}

case class PrintResult(id: Long, success: Boolean, msg: String = "")

class FileClient(conf: PrintcatConfig) extends Actor with ActorLogging {
  val system = context.system
  implicit val ec = system.dispatcher
  implicit val mat = ActorMaterializer()
  var printcatClient: ActorRef = null
  val completedTask = new LinkedBlockingQueue[PrintResult]()

  Future {
    while (true) {
      log.info("wait for completed task...")
      val PrintResult(jobId, success, msg) = completedTask.take
      if (success) printcatClient ! PrintSuceess(jobId) else printcatClient ! PrintError(jobId, msg)
    }
  }
  //(id, success, msg)

  override def receive: Receive = {
    case DownloadFile(jobId, path, f) => if (printcatClient == null) printcatClient = sender
      val request = HttpRequest(uri = s"http://${conf.FILE_SERVER_HOST}:${conf.FILE_SERVER_PORT}/file/exchange/${path}0xfff$jobId")
      val localPath = s"${conf.FILE_CLIENT_LOCAL_DIR}/${UUID.randomUUID}"
      downloadFileTo(jobId, request, localPath, f)
  }

  def downloadFileTo(jobId: Long, request: HttpRequest, destPath: String, f: File => Unit) = {
    val futResp = Http(context.system).singleRequest(request)
    futResp.andThen { case Success(r@HttpResponse(StatusCodes.OK, _, entity, _)) => entity.dataBytes.runWith(FileIO.toPath(Paths.get(destPath))).onComplete { case _ => f(new File(destPath))
      completedTask.put(PrintResult(jobId, success = true))
    }
    case Success(r@HttpResponse(code, _, e, _)) => val msg = s"Download request failed, response code: $code ,error: $e"
      log.info(msg)
      r.discardEntityBytes()
    case Success(o) => val msg = s"Unable to download file!$o"
      completedTask.put(PrintResult(jobId, success = false, msg))
      log.info(msg)
    case Failure(err) => val msg = s"Download failed: ${err.getMessage}"
      completedTask.put(PrintResult(jobId, success = false, msg))
      log.info(msg)
    }
  }
}
