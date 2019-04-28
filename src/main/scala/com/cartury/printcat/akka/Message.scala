package com.cartury.printcat.akka

import java.io.File

case class Register(name: String) extends Serializable
case class RegisterResp(id: Long) extends Serializable
case class GetPrinterList(requestId: Long) extends Serializable
case class Print(requestId: Long, printerId: Long, path: String) extends Serializable
case class DoPrint(jobId: Long, path: String) extends Serializable
case class DownloadFile(jobId: Long, path: String, f: File => Unit) extends Serializable
case class PrintSuceess(jobId: Long) extends Serializable
case class PrintSuceessResp(requestId: Long, printer: String, path: String) extends Serializable
case class PrintError(jobId: Long, err: String) extends Serializable
case class PrintErrorResp(requestId: Long, printer: String, path: String, err: String) extends Serializable
case class PrinterListResult(requestId: Long, printers: String) extends Serializable
case class CheckStatus(id: Long) extends Serializable
case class CheckStatusResp(id: Long) extends Serializable
case class HeartBeat(id: Long) extends Serializable
case object OutOfDateBeat extends Serializable
case object Close extends Serializable