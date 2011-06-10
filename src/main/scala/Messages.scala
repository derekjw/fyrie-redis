package net.fyrie
package redis
package messages

import akka.util.ByteString
import akka.actor.IO
import akka.dispatch.Promise

import types.RedisType

sealed trait Message

case class Request(bytes: ByteString) extends Message
case class MultiRequest(multi: ByteString, cmds: List[(ByteString, Promise[RedisType])], exec: ByteString) extends Message
case object Disconnect extends Message
case object Run extends Message
case class MultiRun(promises: List[Promise[RedisType]]) extends Message
case class Socket(handle: IO.SocketHandle) extends Message
