package com.cartury.printcat.akka

import java.io.File

case class Register(name: String) extends Serializable
case class RegisterResp(printerId: Long) extends Serializable
case object GetPrinterList extends Serializable
case class Print(printerId: Long, path: String) extends Serializable
case class DoPrint(jobId: Long, path: String) extends Serializable
case class DownloadFile(jobId: Long, path: String, f: File => Unit) extends Serializable
case class PrintSuceess(jobId: Long) extends Serializable
case class PrintSuceessResp(printer: String, path: String) extends Serializable
case class PrintError(jobId: Long, err: String) extends Serializable
case class PrintErrorResp(printerId: String, path: String, err: String) extends Serializable
case class PrinterListResult(printers: String) extends Serializable
case class CheckStatus(printerId: Long) extends Serializable
case class CheckStatusResp(printerId: Long) extends Serializable
case class HeartBeat(printerId: Long) extends Serializable
case object OutOfDateBeat extends Serializable
case object Close extends Serializable