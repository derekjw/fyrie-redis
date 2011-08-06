package net.fyrie
package redis
package messages

import akka.util.ByteString
import akka.actor.IO
import akka.dispatch.Promise

import types.RedisType

sealed trait Message
sealed trait RequestMessage extends Message
case class Request(bytes: ByteString) extends RequestMessage
case class MultiRequest(multi: ByteString, cmds: Seq[(ByteString, Promise[RedisType])], exec: ByteString) extends RequestMessage
case object Disconnect extends Message
case object Run extends Message
case class MultiRun(promises: Seq[Promise[RedisType]]) extends Message
case class Socket(handle: IO.SocketHandle) extends Message
case object Received extends Message
