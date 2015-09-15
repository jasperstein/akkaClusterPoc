package com.xebialbas.shool.cluster

import java.io.File

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.util.Timeout
import akka.pattern.ask
import com.xebialbas.shool.cluster.TheSingletonActor.{managername, mgrProps, Ping}
import concurrent.ExecutionContext.Implicits.global
import concurrent.duration._

object ClusterApp extends App {

  val actorSystem = ActorSystem("ClusterSystem")

  new File("target/journal").deleteOnExit()

  Cluster(actorSystem).registerOnMemberUp {
    actorSystem.actorOf(mgrProps(persistent = true), managername)
    val loner = actorSystem.actorOf(TheSingletonActor.proxyProps)

    1 to 100 foreach { i =>
      implicit val timeout: Timeout = 2 seconds
      val result = loner ? new Runnable {
        override def run() { println("Who are you?") }
      }
      result.onComplete(r => println(r + i.toString))
      Thread.sleep(1000)
    }

    loner ! Ping
  }
}
