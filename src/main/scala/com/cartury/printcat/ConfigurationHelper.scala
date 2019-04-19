package com.cartury.printcat

import java.io.InputStream

import org.apache.commons.configuration.PropertiesConfiguration

import scala.collection.JavaConversions._
import scala.util.Random

object ConfigurationHelper {
  def load(stream: InputStream) = {
    val config = new PropertiesConfiguration
    config.load(stream)
    new PrintcatConfig(config)
  }
  def load(path: String) = {
    val config = new PropertiesConfiguration(path)
    config setEncoding "UTF-8"
    new PrintcatConfig(config)
  }
}

class PrintcatConfig(config: PropertiesConfiguration) {

  private val ps = "printcat.server" **
  private val pc = "printcat.client" **
  private val fs = "file.server" **
  private val fc = "file.client" **

  val PRINTCAT_SERVER_HOST = ps ~ ("host", "localhsot")
  val PRINTCAT_SERVER_PORT = ps ~ "port"

  val PRINTCAT_CLIENT_NAME = pc ~ ("name", s"client-${Random nextString 6}")

  val FILE_SERVER_HOST = fs ~ ("host", "localhost")
  val FILE_SERVER_PORT = fs ~ "port"
  val FILE_SERVER_ROOT = fs ~ "root"

  val FILE_CLIENT_PORT = fc ~ "port"
  val FILE_CLIENT_LOCAL_DIR = fc ~ "local.dir"

  implicit class Config(s: String) {
    def prop = config getString s

    def ** = config getKeys s map (e => e.substring(s.length + 1) -> e.prop) toMap
  }

  implicit class Mapper(map: Map[String, String]) {
    def ~(k: String, d: String = "") = map.getOrElse(k, d)
  }


}