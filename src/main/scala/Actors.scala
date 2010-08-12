package net.fyrie
package redis
package akka
package actors

import com.redis._
import commands.{Command}
import replies.{ReplyProxy}

import messages._

import se.scalablesolutions.akka.actor.{Actor, ActorRef}
import Actor.{actorOf}
import se.scalablesolutions.akka.config.{AllForOneStrategy}
import se.scalablesolutions.akka.config.ScalaConfig.{LifeCycle, Permanent}

import se.scalablesolutions.akka.dispatch._

trait ChainedActor {
  this: Actor =>

  def nextActor: ActorRef

  def send(message: Message) {
    if (message.forward) {
      nextActor forward message
    } else {
      nextActor ! message
    }
  }
}

class RedisActor(address: String, port: Int) extends Actor with ChainedActor {
  self.faultHandler = Some(AllForOneStrategy(5, 5000))
  self.trapExit = List(classOf[Exception])

  implicit val dispatcher = Some(new HawtDispatcher(false))

  dispatcher foreach (d => self.dispatcher = d)

  val client = new RedisClient(address, port)

  val transformer = actorOf(new TransformerActor)
  self.startLink(transformer)

  val reader = actorOf(new ReaderActor(transformer, client.reader))
  self.startLink(reader)

  val writer = actorOf(new WriterActor(reader, client.writer))
  self.startLink(writer)

  val preparer = actorOf(new PreparerActor(writer))
  self.startLink(preparer)

  val nextActor = preparer

  def receive = {
    case Request(cmd, frwd) => send(Prepare(cmd, frwd))
  }

  override def shutdown = {
    self.shutdownLinkedActors
    client.disconnect
    super.shutdown
  }

  override def postRestart(reason: Throwable) = client.reconnect

}

class PreparerActor(val nextActor: ActorRef)(implicit dispatcher: Option[MessageDispatcher] = None) extends Actor with ChainedActor {
  self.lifeCycle = Some(LifeCycle(Permanent))

  dispatcher foreach (d => self.dispatcher = d)

  def receive = {
    case Prepare(cmd, frwd) => send(Write(cmd.toBytes, cmd.replyHandler, frwd))
  }
}

class WriterActor(val nextActor: ActorRef, val writer: RedisStreamWriter)(implicit dispatcher: Option[MessageDispatcher] = None) extends Actor with ChainedActor {
  self.lifeCycle = Some(LifeCycle(Permanent))

  dispatcher foreach (d => self.dispatcher = d)

  def receive = {
    case Write(bytes, replyHandler, frwd) =>
      send(Read(replyHandler, frwd))
      writer write bytes
  }
}

class ReaderActor(val nextActor: ActorRef, val reader: RedisStreamReader)(implicit dispatcher: Option[MessageDispatcher] = None) extends Actor with ChainedActor {
  self.lifeCycle = Some(LifeCycle(Permanent))

  dispatcher foreach (d => self.dispatcher = d)

  def handleRedisError(f: => Unit) {
    try {
      f
    } catch {
      case e: RedisErrorException =>
        self.senderFuture.foreach(_.completeWithException(this, e))
    }
  }

  def receive = {
    case Read(replyHandler: Reply[_], false) =>
      handleRedisError{
        reader read replyHandler
        () // Get ClassCastException when an AnyVal is returned, so must return Unit
      }
    case Read(replyHandler: Reply[_], true) =>
      replyHandler match {
        case r: ReplyProxy[_,_] =>
          handleRedisError{
            send(Transform(reader read r.underlying, r, true))
          }
        case _ =>
          handleRedisError{
            self reply (reader read replyHandler)
          }
      }
  }
}

class TransformerActor(implicit dispatcher: Option[MessageDispatcher] = None) extends Actor {
  self.lifeCycle = Some(LifeCycle(Permanent))

  dispatcher foreach (d => self.dispatcher = d)

  def receive = {
    case t: Transform[_,_] =>
      self reply t.execute
  }
}
