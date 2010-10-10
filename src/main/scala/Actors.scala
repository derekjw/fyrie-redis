package net.fyrie
package redis
package akka
package actors

import messages._

import handlers.{Handler}

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
  self.faultHandler = AllForOneStrategy(List(classOf[Exception]), 5, 5000)

  self.dispatcher = implicitly

  val client = new RedisClient(address, port)

  val reader = actorOf(new ReaderActor(client.reader))
  self.startLink(reader)

  val writer = actorOf(new WriterActor(reader, client.writer))
  self.startLink(writer)

  val nextActor = writer

  def receive = {
    case r: Request => send(r)
  }

  override def postStop = {
    self.shutdownLinkedActors
    client.disconnect
  }

  override def postRestart(reason: Throwable) = client.reconnect

}

class RedisWorkerActor(address: String, port: Int)(implicit dispatcher: MessageDispatcher) extends Actor {

  self.dispatcher = implicitly

  val client = new RedisClient(address, port)

  override def postStop = {
    client.disconnect
  }

  def receive = {
    case Work(command) =>
      self.reply_?(client send command)
  }
  
}

class WriterActor(val nextActor: ActorRef, val writer: RedisStreamWriter)(implicit dispatcher: MessageDispatcher) extends Actor with ChainedActor {
  self.lifeCycle = Permanent

  self.dispatcher = implicitly

  def receive = {
    case Request(bytes, handler, forward) =>
      send(Response(handler, forward))
      writer write bytes
  }
}

class ReaderActor(val reader: RedisStreamReader)(implicit dispatcher: MessageDispatcher) extends Actor {
  self.lifeCycle = Permanent

  self.dispatcher = implicitly

  val baseHandlers = new handlers.BaseHandlers

  def handleRedisError(f: => Unit) {
    try {
      baseHandlers.forceLazyResults
      f
      baseHandlers.forceLazyResults
    } catch {
      case e: RedisErrorException =>
        println("Got Redis Exception: "+e)
        self.senderFuture.map(_.completeWithException(e)).getOrElse(log.error(e.toString))
    }
  }

  def receive = {
    case Response(handler, false) =>
      handleRedisError( handler(reader, baseHandlers) )
    case Response(handler, true) =>
      handleRedisError( self reply handler(reader, baseHandlers) )
  }
}
