package com.cartury.printcat.akka

import akka.actor.{Actor, ActorLogging}
import akka.util.Timeout

import scala.concurrent.duration._
trait ActorEnv extends Actor with ActorLogging{
  protected implicit lazy val system = context.system
  protected implicit lazy val ec = system.dispatcher
  protected implicit lazy val timeout = Timeout(10 seconds)
}
