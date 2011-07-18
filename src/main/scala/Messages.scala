package net.fyrie
package redis
package messages

import akka.util.ByteString
import akka.actor.IO
import akka.dispatch.DefaultCompletableFuture

import types.RedisType

sealed trait Message
sealed trait RequestMessage extends Message
case class Request(bytes: ByteString) extends RequestMessage
case class MultiRequest(multi: ByteString, cmds: Seq[(ByteString, DefaultCompletableFuture[RedisType])], exec: ByteString) extends RequestMessage
case object Disconnect extends Message
case object Run extends Message
case class MultiRun(promises: Seq[DefaultCompletableFuture[RedisType]]) extends Message
case class Socket(handle: IO.SocketHandle) extends Message
case object Received extends Message
