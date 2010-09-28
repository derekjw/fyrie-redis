package net.fyrie
package redis
package akka
package actors

import messages._

import handlers.{Handler, BulkHandler}

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

  val deserializer = actorOf(new DeserializerActor)
  self.startLink(deserializer)

  val reader = actorOf(new ReaderActor(deserializer, client.reader))
  self.startLink(reader)

  val writer = actorOf(new WriterActor(reader, client.writer))
  self.startLink(writer)

  val serializer = actorOf(new SerializerActor(writer))
  self.startLink(serializer)

  val nextActor = serializer

  def receive = {
    case r: Request[_,_] =>
      send(Serialize(r.command, r.forward))
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
    case Work(command) =>
      self.reply_?(client send command)
  }
  
}

class SerializerActor(val nextActor: ActorRef)(implicit dispatcher: MessageDispatcher) extends Actor with ChainedActor {
  self.lifeCycle = Some(LifeCycle(Permanent))

  self.dispatcher = implicitly

  def receive = {
    case p: Serialize[_,_] =>
      send(Write(p.command.toBytes, p.command.handler, p.forward))
  }
}

class WriterActor(val nextActor: ActorRef, val writer: RedisStreamWriter)(implicit dispatcher: MessageDispatcher) extends Actor with ChainedActor {
  self.lifeCycle = Some(LifeCycle(Permanent))

  self.dispatcher = implicitly

  def receive = {
    case w: Write[_,_] =>
      send(Read(w.handler, w.forward))
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
        println("Got Redis Exception: "+e)
        self.senderFuture.map(_.completeWithException(e)).getOrElse(log.error(e.toString))
    }
  }

  def receive = {
    case r: Read[_,_] if !r.forward =>
      handleRedisError{
        r.handler.read(reader)
        () // Get ClassCastException when an AnyVal is returned, so must return Unit
      }
    case Read(h: BulkHandler[_,_], true) =>
      handleRedisError{
        send(Deserialize(h.read(reader), h))
      }
    case Read(h: Handler[_,_], true) =>
      handleRedisError{
        self reply h(reader)
      }
  }
}

class DeserializerActor(implicit dispatcher: MessageDispatcher) extends Actor {
  self.lifeCycle = Some(LifeCycle(Permanent))

  self.dispatcher = implicitly

  def receive = {
    case t: Deserialize[_,_] =>
      self reply t.parse
  }
}
