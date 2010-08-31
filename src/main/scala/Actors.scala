package net.fyrie
package redis
package akka
package actors

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

class RedisPipelineActor(address: String, port: Int)(implicit dispatcher: MessageDispatcher) extends Actor with ChainedActor {
  self.faultHandler = Some(AllForOneStrategy(5, 5000))
  self.trapExit = List(classOf[Exception])

  self.dispatcher = implicitly

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
    case r: Request[_,_] => send(Prepare(r.command, r.forward, r.transform))
  }

  override def shutdown = {
    self.shutdownLinkedActors
    client.disconnect
  }

  override def postRestart(reason: Throwable) = client.reconnect

}

class RedisWorkerActor(address: String, port: Int)(implicit dispatcher: MessageDispatcher) extends Actor {

  self.dispatcher = implicitly

  val client = new RedisClient(address, port)

  override def shutdown = {
    client.disconnect
  }

  def receive = {
    case Work(command, transform) =>
      self.reply_?(transform(client send command))
  }
  
}

class PreparerActor(val nextActor: ActorRef)(implicit dispatcher: MessageDispatcher) extends Actor with ChainedActor {
  self.lifeCycle = Some(LifeCycle(Permanent))

  self.dispatcher = implicitly

  def receive = {
    case p: Prepare[_,_] => send(Write(p.command.toBytes, p.command.replyHandler, p.forward, p.transform))
  }
}

class WriterActor(val nextActor: ActorRef, val writer: RedisStreamWriter)(implicit dispatcher: MessageDispatcher) extends Actor with ChainedActor {
  self.lifeCycle = Some(LifeCycle(Permanent))

  self.dispatcher = implicitly

  def receive = {
    case w: Write[_,_] =>
      send(Read(w.replyHandler, w.forward, w.transform))
      writer write w.bytes
  }
}

class ReaderActor(val nextActor: ActorRef, val reader: RedisStreamReader)(implicit dispatcher: MessageDispatcher) extends Actor with ChainedActor {
  self.lifeCycle = Some(LifeCycle(Permanent))

  self.dispatcher = implicitly

  def handleRedisError(f: => Unit) {
    try {
      f
    } catch {
      case e: RedisErrorException =>
        self.senderFuture.map(_.completeWithException(e)).getOrElse(log.error(e.toString))
    }
  }

  def receive = {
    case r: Read[_,_] if !r.forward =>
      handleRedisError{
        reader read r.replyHandler
        () // Get ClassCastException when an AnyVal is returned, so must return Unit
      }
    case r: Read[_,_] =>
      handleRedisError{
        send(Transform(reader read r.replyHandler, true, r.transform))
      }
  }
}

class TransformerActor(implicit dispatcher: MessageDispatcher) extends Actor {
  self.lifeCycle = Some(LifeCycle(Permanent))

  self.dispatcher = implicitly

  def receive = {
    case t: Transform[_,_] =>
      self reply t.transform(t.data)
  }
}
