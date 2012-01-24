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
  import Constants._
  import Iteratees._

  val log = Logging(context.system, this)

  var sockets: Vector[IO.SocketHandle] = _

  var nextSocket: Int = 0

  val state = IO.IterateeRef.Map.async[IO.Handle]()(context.dispatcher)

  var results = 0L
  var resultCallbacks = Seq.empty[(Long, Long) ⇒ Unit]
  var requestCallbacks = Seq.empty[(Long, Long) ⇒ Unit]

  val waiting = Queue.empty[RequestMessage]
  var requests = 0L

  override def preStart = {
    log info ("Connecting")
    sockets = Vector.fill(config.connections)(IOManager(context.system).connect(host, port))
  }

  def receive = {

    case req @ Request(requestor, bytes) ⇒
      val socket = sockets(nextSocket)
      nextSocket = (nextSocket + 1) % config.connections
      socket write bytes
      onRequest(req)
      for {
        _ ← state(socket)
        result ← readResult
      } yield {
        requestor respond result
        onResult()
      }

    case req @ MultiRequest(requestor, _, cmds, _) ⇒
      val socket = sockets(nextSocket)
      nextSocket = (nextSocket + 1) % config.connections
      sendMulti(req, socket)
      onRequest(req)
      for {
        _ ← state(socket)
        _ ← readResult
        _ ← IO.fold((), cmds.map(_._2))((_, p) ⇒ readResult map (p success))
        exec ← readResult
      } yield {
        requestor respond exec
        onResult()
      }

    case IO.Read(handle, bytes) ⇒
      state(handle)(IO Chunk bytes)

    case RequestCallback(callback) ⇒
      requestCallbacks +:= callback

    case ResultCallback(callback) ⇒
      resultCallbacks +:= callback
    /*
    case IO.Closed(handle, Some(cause: ConnectException)) if socket == handle && config.autoReconnect ⇒
      log info ("Connection refused, retrying in 1 second")
      context.system.scheduler.scheduleOnce(1 second, self, IO.Closed(handle, None))

    case IO.Closed(handle, cause) if socket == handle && config.autoReconnect ⇒
      log info ("Reconnecting" + (cause map (e ⇒ ", cause: " + e.toString) getOrElse ""))
      socket = IOManager(context.system).connect(host, port)
      if (config.retryOnReconnect) {
        waiting foreach {
          case req: Request      ⇒ socket write req.bytes
          case req: MultiRequest ⇒ sendMulti(req)
        }
        if (waiting.nonEmpty) log info ("Retrying " + waiting.length + " commands")
      } */

    case IO.Closed(handle, cause) if sockets contains handle ⇒
      log info ("Connection closed" + (cause map (e ⇒ ", cause: " + e.toString) getOrElse ""))
      // FIXME: stop actors
      // sendToSupervisor(Disconnect)
      state(handle)(IO EOF cause)

    case Received ⇒
    //waiting.dequeue

  }

  def onRequest(req: RequestMessage): Unit = {
    //if (config.retryOnReconnect) waiting enqueue req
    requests += 1L
    if (requestCallbacks.nonEmpty) {
      val atTime = System.currentTimeMillis
      requestCallbacks foreach (_(requests, atTime))
    }
  }

  def sendMulti(req: MultiRequest, socket: IO.SocketHandle): Unit = {
    socket write req.multi
    req.cmds foreach { cmd ⇒
      socket write cmd._1
    }
    socket write req.exec
  }

  def onResult() {
    //if (config.retryOnReconnect) self ! Received
    results += 1L
    if (resultCallbacks.nonEmpty) {
      val atTime = System.currentTimeMillis
      resultCallbacks foreach (_(results, atTime))
    }
  }

  override def postStop() {
    log info ("Shutting down")
    sockets.foreach(_.close)
    // TODO: Complete all waiting requests with a RedisConnectionException
  }

}

private[redis] final class RedisSubscriberSession(listener: ActorRef)(host: String, port: Int, config: RedisClientConfig) extends Actor {
  import Iteratees._

  val log = Logging(context.system, this)

  var socket: IO.SocketHandle = _
  val state = IO.IterateeRef.async()(context.dispatcher)

  val client = new RedisClientSub(self, config, context.system)

  override def preStart = {
    log info ("Connecting")
    socket = IOManager(context.system).connect(host, port)
    state flatMap (_ ⇒ subscriber(listener))
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

    case IO.Read(handle, bytes) ⇒
      state(IO Chunk bytes)

    case IO.Closed(handle, cause) if socket == handle ⇒
      log info ("Connection closed" + (cause map (e ⇒ ", cause: " + e.toString) getOrElse ""))
      // FIXME: stop actors
      // sendToSupervisor(Disconnect)
      state(IO EOF cause)
  }

  override def postStop() {
    socket.close
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
