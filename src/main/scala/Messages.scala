package net.fyrie
package redis
package akka
package messages

import handlers.{Handler}

trait Message {
  val forward: Boolean
}

case class Request(bytes: Array[Byte], handler: Handler[_], forward: Boolean) extends Message

case class Response(handler: Handler[_], forward: Boolean) extends Message

case class Work(command: Command[_])
