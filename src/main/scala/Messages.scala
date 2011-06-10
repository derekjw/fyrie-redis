package net.fyrie
package redis
package messages

import akka.util.ByteString
import akka.actor.IO

sealed trait Message

case class Request(bytes: ByteString) extends Message
case object Disconnect extends Message
case object Run extends Message
case class Socket(handle: IO.SocketHandle) extends Message
