package net.fyrie
package redis
package actors

import messages._
import types._
import pubsub._
import protocol._

import akka.actor._
import akka.util.ByteString
import akka.util.duration._
import akka.event.Logging

import java.util.concurrent.TimeUnit
import java.net.ConnectException

import scala.collection.mutable.Queue

private[redis] final class RedisClientSession(host: String, port: Int, config: RedisClientConfig) extends Actor {

  val log = Logging(context.system, this)

  var socket: IO.SocketHandle = _
  val worker: ActorRef = context.actorOf(Props(new RedisClientWorker(host, port, config, self)))

  val waiting = Queue.empty[RequestMessage]
  var requests = 0L
  var requestCallbacks = Seq.empty[(Long, Long) ⇒ Unit]

  override def preStart = {
    log info ("Connecting")
    socket = IO.connect(host, port, worker)
    worker ! Socket(socket)
  }

  def receive = {

    case RequestCallback(callback) ⇒
      requestCallbacks +:= callback

    case msg: ResultCallback ⇒
      worker ! msg

    case req @ Request(requestor, bytes) ⇒
      worker ! requestor
      socket write bytes
      onRequest(req)

    case req: MultiRequest ⇒
      worker ! MultiRun(req.requestor, req.cmds.map(_._2))
      sendMulti(req)
      onRequest(req)

    case Received ⇒
      waiting.dequeue

    case Socket(handle) ⇒
      socket = handle
      if (config.retryOnReconnect) {
        waiting foreach {
          case req: Request      ⇒ socket write req.bytes
          case req: MultiRequest ⇒ sendMulti(req)
        }
        if (waiting.nonEmpty) log info ("Retrying " + waiting.length + " commands")
      }

  }

  def onRequest(req: RequestMessage): Unit = {
    if (config.retryOnReconnect) waiting enqueue req
    requests += 1L
    if (requestCallbacks.nonEmpty) {
      val atTime = System.currentTimeMillis
      requestCallbacks foreach (_(requests, atTime))
    }
  }

  def sendMulti(req: MultiRequest): Unit = {
    socket write req.multi
    req.cmds foreach { cmd ⇒
      socket write cmd._1
    }
    socket write req.exec
  }

}

private[redis] final class RedisSubscriberSession(listener: ActorRef)(host: String, port: Int, config: RedisClientConfig) extends Actor {

  val log = Logging(context.system, this)

  var socket: IO.SocketHandle = _
  val worker = context.actorOf(Props(new RedisClientWorker(host, port, config, self)))

  val client = new RedisClientSub(self, config, context.system)

  override def preStart = {
    log info ("Connecting")
    socket = IO.connect(host, port, worker)
    worker ! Socket(socket)
    worker ! Subscriber(listener)
  }

  def receive = {

    case Subscribe(channels) ⇒
      socket write (client.subscribe(channels))

    case Unsubscribe(channels) ⇒
      socket write (client.unsubscribe(channels))

    case PSubscribe(patterns) ⇒
      socket write (client.psubscribe(patterns))

    case PUnsubscribe(patterns) ⇒
      socket write (client.punsubscribe(patterns))

  }

  override def postStop() {
    socket.close
  }

}

private[redis] final class RedisClientWorker(host: String, port: Int, config: RedisClientConfig, parent: ActorRef) extends Actor {
  import Constants._
  import Iteratees._

  val log = Logging(context.system, this)

  var socket: IO.SocketHandle = _

  val state = IO.IterateeRef.sync()

  var results = 0L
  var resultCallbacks = Seq.empty[(Long, Long) ⇒ Unit]

  def receive = {

    case IO.Read(handle, bytes) ⇒
      state(IO Chunk bytes)

    case IO.Connected(handle) ⇒

    case ResultCallback(callback) ⇒
      resultCallbacks +:= callback

    case Socket(handle) ⇒
      socket = handle

    case IO.Closed(handle, Some(cause: ConnectException)) if socket == handle && config.autoReconnect ⇒
      log info ("Connection refused, retrying in 1 second")
      context.system.scheduler.scheduleOnce(1 second, self, IO.Closed(handle, None))

    case IO.Closed(handle, cause) if socket == handle && config.autoReconnect ⇒
      log info ("Reconnecting" + (cause map (e ⇒ ", cause: " + e.toString) getOrElse ""))
      socket = IO.connect(host, port, self)
      sendToSupervisor(Socket(socket))

    case IO.Closed(handle, cause) if socket == handle ⇒
      log info ("Connection closed" + (cause map (e ⇒ ", cause: " + e.toString) getOrElse ""))
      // FIXME: stop actors
      // sendToSupervisor(Disconnect)
      state(IO EOF cause)

    case req: Requestor ⇒
      for {
        _ ← state
        result ← readResult
      } yield {
        req respond result
        onResult()
      }

    case MultiRun(req, promises) ⇒
      for {
        _ ← state
        _ ← readResult
        _ ← IO.fold((), promises)((_, p) ⇒ readResult map (p success))
        exec ← readResult
      } yield {
        req respond exec
        onResult()
      }

    case Subscriber(listener) ⇒
      state flatMap (_ ⇒ subscriber(listener))

  }

  override def postStop() {
    // TODO: Complete all waiting requests with a RedisConnectionException
    socket.close
  }

  def onResult() {
    if (config.retryOnReconnect) sendToSupervisor(Received)
    results += 1L
    if (resultCallbacks.nonEmpty) {
      val atTime = System.currentTimeMillis
      resultCallbacks foreach (_(results, atTime))
    }
  }

  def sendToSupervisor(msg: Any) {
    try {
      parent ! msg
    } catch {
      case e: ActorInitializationException ⇒ // ignore, probably shutting down
    }
  }

  def subscriber(listener: ActorRef): IO.Iteratee[Unit] = readResult flatMap { result ⇒
    result match {
      case RedisMulti(Some(List(RedisBulk(Some(Constants.message)), RedisBulk(Some(channel)), RedisBulk(Some(message))))) ⇒
        listener ! pubsub.Message(channel, message)
      case RedisMulti(Some(List(RedisBulk(Some(Constants.pmessage)), RedisBulk(Some(pattern)), RedisBulk(Some(channel)), RedisBulk(Some(message))))) ⇒
        listener ! pubsub.PMessage(pattern, channel, message)
      case RedisMulti(Some(List(RedisBulk(Some(Constants.subscribe)), RedisBulk(Some(channel)), RedisInteger(count)))) ⇒
        listener ! pubsub.Subscribed(channel, count)
      case RedisMulti(Some(List(RedisBulk(Some(Constants.unsubscribe)), RedisBulk(Some(channel)), RedisInteger(count)))) ⇒
        listener ! pubsub.Unsubscribed(channel, count)
      case RedisMulti(Some(List(RedisBulk(Some(Constants.psubscribe)), RedisBulk(Some(pattern)), RedisInteger(count)))) ⇒
        listener ! pubsub.PSubscribed(pattern, count)
      case RedisMulti(Some(List(RedisBulk(Some(Constants.punsubscribe)), RedisBulk(Some(pattern)), RedisInteger(count)))) ⇒
        listener ! pubsub.PUnsubscribed(pattern, count)
      case other ⇒
        throw RedisProtocolException("Unexpected response")
    }
    subscriber(listener)
  }

}
