package net.fyrie
package redis
package akka

import com.redis._
import commands.{Command}
import replies.ReplyProxy

import se.scalablesolutions.akka.actor.{Actor, ActorRef}
import Actor.{actorOf}
import se.scalablesolutions.akka.config.{AllForOneStrategy}
import se.scalablesolutions.akka.config.ScalaConfig.{LifeCycle, Permanent}

import se.scalablesolutions.akka.dispatch._

class AkkaRedisClient(address: String = "localhost", port: Int = 6379) {
  val actorRef = actorOf(new RedisActor(address, port)).start

  def !(command: Command[_])(implicit sender: Option[ActorRef] = None): Unit = actorRef.!((OneWay, command))(sender)
  def !![T](command: Command[T])(implicit sender: Option[ActorRef] = None): Option[T] = this.!!!(command)(sender).await.result
  def !!![T](command: Command[T])(implicit sender: Option[ActorRef] = None): Future[T] = actorRef.!!!((TwoWay,command))(sender)

  def stop = actorRef.stop
}

case object OneWay
case object TwoWay

class RedisActor(address: String, port: Int) extends Actor {
  self.faultHandler = Some(AllForOneStrategy(5, 5000))
  self.trapExit = List(classOf[Exception])

  val dispatcher = new HawtDispatcher(false)

  self.dispatcher = dispatcher

  val redisClient = new RedisClient(address, port)

  val serializerActor = actorOf(new RedisSerializer)
  self.startLink(serializerActor)

  val deserializerActor = actorOf(new RedisDeserializer)
  self.startLink(deserializerActor)

  val writerActor = actorOf(new RedisWriterActor)
  self.startLink(writerActor)

  val readerActor = actorOf(new RedisReaderActor)
  self.startLink(readerActor)

  def receive = {
    case msg @ (OneWay, cmd: Command[_]) => serializerActor ! msg
    case msg @ (TwoWay, cmd: Command[_]) => serializerActor forward msg
  }

  override def shutdown = {
    self.shutdownLinkedActors
    super.shutdown
  }

  override def postRestart(reason: Throwable) = redisClient.reconnect

  class RedisSerializer extends Actor {
    self.lifeCycle = Some(LifeCycle(Permanent))

    self.dispatcher = dispatcher

    def receive = {
      case (OneWay, cmd: Command[_]) => writerActor ! ((OneWay, cmd.toBytes, cmd.replyHandler))
      case (TwoWay, cmd: Command[_]) => writerActor forward ((TwoWay, cmd.toBytes, cmd.replyHandler))
    }
  }

  class RedisWriterActor extends Actor {
    self.lifeCycle = Some(LifeCycle(Permanent))

    self.dispatcher = dispatcher

    def receive = {
      case (OneWay, bytes: Array[Byte], replyHandler: Reply[_]) =>
        readerActor ! ((OneWay, replyHandler))
        redisClient.writer write bytes
      case (TwoWay, bytes: Array[Byte], replyHandler: Reply[_]) =>
        readerActor forward ((TwoWay, replyHandler))
        redisClient.writer write bytes        
    }
  }

  class RedisReaderActor extends Actor {
    self.lifeCycle = Some(LifeCycle(Permanent))

    self.dispatcher = dispatcher

    def receive = {
      case (OneWay, replyHandler: ReplyProxy[_,_]) =>
        redisClient.reader read replyHandler.underlying
      case (TwoWay, replyHandler: ReplyProxy[_,_]) =>
        deserializerActor forward ((redisClient.reader read replyHandler.underlying, replyHandler))
      case (OneWay, replyHandler: Reply[_]) =>
        redisClient.reader read replyHandler
      case (TwoWay, replyHandler: Reply[_]) =>
        self reply (redisClient.reader read replyHandler)
    }
  }

  class RedisDeserializer extends Actor {
    self.lifeCycle = Some(LifeCycle(Permanent))

    self.dispatcher = dispatcher

    def receive = {
      case (x, r: ReplyProxy[_,_]) => self reply (r.asInstanceOf[ReplyProxy[Any,Any]] transform x) // FIXME
    }
  }

}


