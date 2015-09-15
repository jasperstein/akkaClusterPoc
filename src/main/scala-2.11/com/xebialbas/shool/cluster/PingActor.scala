package com.xebialbas.shool.cluster

import akka.actor.{Actor, Props}

object PingActor {
  val name = "ping"
  val path = s"/user/$name"
  val props = Props(new PingActor)
}

class PingActor extends Actor {
  override def receive: Receive = {
    case msg =>
      println(s"got $msg from ${sender()}")
      sender() ! s"I'm ${self.path.toStringWithAddress(AddressExtension(context.system).address)}"
  }
}
