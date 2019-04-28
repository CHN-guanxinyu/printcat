package com.cartury.printcat.akka

import java.io.File

case object GetEnv
case class Register(name: String) extends Serializable
case class RegisterResp(printerId: Long) extends Serializable
case object GetPrinterList extends Serializable
case class Print(printerId: Long, path: String) extends Serializable
case class DoPrint(path: String) extends Serializable
case class DownloadFile(path: String, f: File => Unit) extends Serializable
case object DownloadSuceess extends Serializable
case class DownloadError(msg: String) extends Serializable
case object PrintSuceess extends Serializable
case object PrintSuceessResp extends Serializable
case class PrintError(err: String) extends Serializable
case class PrintErrorResp(err: String) extends Serializable
case class PrinterListResult(printers: String) extends Serializable
case class CheckStatus(printerId: Long) extends Serializable
case class CheckStatusResp(printerId: Long) extends Serializable
case class HeartBeat(printerId: Long) extends Serializable
case object OutOfDateBeat extends Serializable
case object Close extends Serializable