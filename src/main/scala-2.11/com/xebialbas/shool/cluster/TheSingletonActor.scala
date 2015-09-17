package com.xebialbas.shool.cluster

import java.util.Date
import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, Actor, Props}
import akka.contrib.pattern.{ClusterSingletonManager, ClusterSingletonProxy}
import akka.persistence._
import com.xebialbas.shool.cluster.TheSingletonActor.{Tick, Ping}
import us.theatr.akka.quartz.{AddCronScheduleFailure, AddCronScheduleSuccess}

import scala.concurrent.duration.FiniteDuration

object TheSingletonActor {
  def props = Props[TheSingletonActor]

  val name = "TheSingleton"
  val managername = "manager"

  case object DoExit

  case object Ping

  case object Tick

  def mgrProps(persistent: Boolean = false) = ClusterSingletonManager.props(
    singletonProps = if (persistent) Props[TheSingletonPersistentActor] else Props[TheSingletonActor],
    singletonName = name,
    terminationMessage = DoExit,
    role = None
  )

  def proxyProps = ClusterSingletonProxy.props(
    singletonPath = s"/user/$managername/$name",
    role = None
  )
}


class TheSingletonActor extends Actor {

  import context._

  var count = 0

  override def preStart(): Unit = {
    println(s"***************** ${new Date} $self, count = $count")
  }

  override def receive = {
    case Ping =>
      count = count + 1
      println(s"$count Ping!!! ${new Date}")
      context.system.scheduler.scheduleOnce(FiniteDuration(15, TimeUnit.SECONDS), self, Ping)
    case msg =>
      count = count + 1
      println(s"$count The singleton got: $msg from ${sender()}")
      sender() ! self
  }

  override def postStop(): Unit = {
    println(s"################ ${new Date} $self, final count = $count")
  }
}


class TheSingletonPersistentActor extends PersistentActor with AtLeastOnceDelivery {

  import context._

  var count = 0

  var quartzActor: ActorRef = _

  override def receiveCommand = {
    case AddCronScheduleSuccess => println("Cron success")
    case AddCronScheduleFailure => println("Cron fail")
    case Tick => println("Tick")
    case Ping =>
      println("Rcvd: Ping")
      count = count + 1
      saveSnapshot(count)
      println(s"$count Ping!!! ${new Date}")
      context.system.scheduler.scheduleOnce(FiniteDuration(15, TimeUnit.SECONDS), self, Ping)
    //    case Ping =>
    //      println("Rcvd: Ping")
    //      persist(Ping) { _ =>
    //        count = count + 1
    //        println(s"$count Ping!!! ${new Date}")
    //        context.system.scheduler.scheduleOnce(FiniteDuration(15, TimeUnit.SECONDS), self, Ping)
    //      }
    case SaveSnapshotSuccess(md) => println(s"Save snapshot success, yay! $md}")
    case SaveSnapshotFailure(md, reason) => println(s"Save snapshot fail, bummer! $md, $reason}")
    case msg =>
      println(s"Rcvd: ${msg}")
      count = count + 1
      saveSnapshot(count)
      println(s"$count The singleton got: $msg from ${sender()}")
      sender() ! self
    //    case msg =>
    //      println(s"Rcvd: ${msg}")
    //      persist(msg) { _ =>
    //        count = count + 1
    //        println(s"$count The singleton got: $msg from ${sender()}")
    //        sender() ! self
    //      }
  }

  override def postStop(): Unit = {
    println(s"################ ${new Date} $self, final count = $count")
  }

  override def receiveRecover: Receive = {
    case RecoveryCompleted =>
      import us.theatr.akka.quartz._
      quartzActor = context.actorOf(Props[QuartzActor])
      quartzActor ! AddCronSchedule(self, "0/5 * * * * ?", Tick)
      println("Recovery done.")
    case SnapshotOffer(md, snapshotcount) =>
      println(s"Snapshot: metadata=${md}, snapshot=${snapshotcount}")
      count = snapshotcount.asInstanceOf[Int]
    case msg =>
      println(s"Recovery: $msg")
      count = count + 1
  }

  override def persistenceId: String = this.getClass.getCanonicalName
}
