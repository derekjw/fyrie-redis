package net.fyrie
package redis
package messages

import akka.util.ByteString
import akka.actor.{ IO, ActorRef }
import akka.dispatch.Promise

import types.RedisType
import serialization.Store

private[redis] sealed trait Requestor {
  def respond(value: RedisType): Unit
  def failure(err: Exception): Unit
}
private[redis] case object NoRequestor extends Requestor {
  def respond(value: RedisType) = ()
  def failure(err: Exception) = ()
}
private[redis] case class PromiseRequestor(promise: Promise[RedisType]) extends Requestor {
  def respond(value: RedisType) = promise success value
  def failure(err: Exception) = promise failure err
}

private[redis] sealed trait Message
private[redis] sealed trait RequestMessage extends Message
private[redis] case class Request(requestor: Requestor, bytes: ByteString) extends RequestMessage
private[redis] case class MultiRequest(requestor: Requestor, multi: ByteString, cmds: Seq[(ByteString, Promise[RedisType])], exec: ByteString) extends RequestMessage
private[redis] case object Disconnect extends Message
private[redis] case class MultiRun(requestor: Requestor, promises: Seq[Promise[RedisType]]) extends Message
private[redis] case class Socket(handle: IO.SocketHandle) extends Message
private[redis] case object Received extends Message
private[redis] case class Subscriber(listener: ActorRef) extends Message
private[redis] case class RequestCallback(callback: (Long, Long) ⇒ Unit) extends Message
private[redis] case class ResultCallback(callback: (Long, Long) ⇒ Unit) extends Message
private[redis] case class ReleaseClient(client: RedisClientPoolWorker) extends Message
private[redis] case class RequestClient(promise: Promise[RedisClientPoolWorker]) extends Message
