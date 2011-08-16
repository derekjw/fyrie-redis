package net.fyrie
package redis
package actors

import messages._
import types._

import akka.actor.{ Actor, ActorRef, IO, IOManager, Scheduler, ActorInitializationException }
import Actor.{ actorOf }
import akka.util.ByteString
import akka.util.cps._
import akka.event.EventHandler

import java.util.concurrent.TimeUnit
import java.net.ConnectException

import scala.util.continuations._

import scala.collection.mutable.Queue

private[redis] final class RedisClientSession(ioManager: ActorRef, host: String, port: Int, config: RedisClientConfig) extends Actor {

  var socket: IO.SocketHandle = _
  var worker: ActorRef = _

  val waiting = Queue.empty[RequestMessage]

  override def preStart = {
    EventHandler info (this, "Connecting")
    worker = actorOf(new RedisClientWorker(ioManager, host, port, config))
    self startLink worker
    socket = IO.connect(ioManager, host, port, worker)
    worker ! Socket(socket)
  }

  def receive = {

    case req: Request =>
      socket write req.bytes
      worker forward Run
      if (config.retryOnReconnect) waiting enqueue req

    case req: MultiRequest =>
      sendMulti(req)
      worker forward MultiRun(req.cmds.map(_._2))
      if (config.retryOnReconnect) waiting enqueue req

    case Received =>
      waiting.dequeue

    case Socket(handle) =>
      socket = handle
      if (config.retryOnReconnect) {
        waiting foreach {
          case req: Request => socket write req.bytes
          case req: MultiRequest => sendMulti(req)
        }
        if (waiting.nonEmpty) EventHandler info (this, "Retrying " + waiting.length + " commands")
      }

    case Disconnect =>
      EventHandler info (this, "Shutting down")
      worker ! Disconnect
      self.stop()

  }

  def sendMulti(req: MultiRequest): Unit = {
    socket write req.multi
    req.cmds foreach { cmd =>
      socket write cmd._1
    }
    socket write req.exec
  }

}

private[redis] final class RedisClientWorker(ioManager: ActorRef, host: String, port: Int, config: RedisClientConfig) extends Actor with IO {
  import Protocol._

  var socket: IO.SocketHandle = _

  def receiveIO: ReceiveIO = {

    case Socket(handle) =>
      socket = handle

    case IO.Closed(handle, Some(cause: ConnectException)) if socket == handle && config.autoReconnect =>
      EventHandler info (this, "Connection refused, retrying in 1 second")
      Scheduler.scheduleOnce(self, IO.Closed(handle, None), 1, TimeUnit.SECONDS)
      retry

    case IO.Closed(handle, cause) if socket == handle && config.autoReconnect =>
      EventHandler info (this, "Reconnecting" + (cause map (e => ", cause: " + e.toString) getOrElse ""))
      socket = IO.connect(ioManager, host, port, self)
      sendToSupervisor(Socket(socket))
      retry

    case IO.Closed(handle, cause) if socket == handle =>
      EventHandler info (this, "Connection closed" + (cause map (e => ", cause: " + e.toString) getOrElse ""))
      sendToSupervisor(Disconnect)

    case Run =>
      val result = readResult
      self tryReply result
      if (config.retryOnReconnect) sendToSupervisor(Received)

    case msg: MultiRun =>
      val multi = readResult
      var promises = msg.promises
      whileC(promises.nonEmpty) {
        val promise = promises.head
        promises = promises.tail
        val result = readResult
        promise completeWithResult result
      }
      val exec = readResult
      self tryReply exec
      if (config.retryOnReconnect) sendToSupervisor(Received)

    case Disconnect =>
      // TODO: Complete all waiting requests with a RedisConnectionException
      socket.close
      self.stop()

  }

  def sendToSupervisor(msg: Any) {
    try {
      self.supervisor foreach (_ ! msg)
    } catch {
      case e: ActorInitializationException => // ignore, probably shutting down
    }
  }

  def readResult: RedisType @cps[IO.IOSuspendable[Any]] = {
    val resultType = socket read 1
    resultType.head.toChar match {
      case '+' => readString
      case '-' => readError
      case ':' => readInteger
      case '$' => readBulk
      case '*' => readMulti
      case x => throw RedisProtocolException("Invalid result type: " + x.toByte)
    }
  }

  def readString = {
    val bytes = socket read EOL
    RedisString(bytes.utf8String)
  }

  def readError = {
    val bytes = socket read EOL
    RedisError(bytes.utf8String)
  }

  def readInteger = {
    val bytes = socket read EOL
    RedisInteger(bytes.utf8String.toLong)
  }

  def readBulk = {
    val length = socket read EOL
    matchC(length.utf8String.toInt) {
      case -1 => RedisBulk.notfound
      case 0 => RedisBulk.empty
      case n =>
        val bytes = socket read n
        socket read EOL
        RedisBulk(Some(bytes))
    }
  }

  def readMulti = {
    val count = socket read EOL
    matchC(count.utf8String.toInt) {
      case -1 => RedisMulti.notfound
      case 0 => RedisMulti.empty
      case n =>
        var result = new Array[RedisType](n)
        var i = 0
        repeatC(n) {
          result(i) = readResult
          i += 1
        }
        RedisMulti(Some(result.toList))
    }
  }
}
