package net.fyrie
package redis
package akka
package messages

import handlers.{Handler, BulkHandler}

trait Message {
  val forward: Boolean
}

case class Request[A, B](command: Command[A, B], forward: Boolean) extends Message

case class Serialize[A, B](command: Command[A, B], forward: Boolean) extends Message

case class Write[A, B](bytes: Array[Byte], handler: Handler[A, B], forward: Boolean) extends Message

case class Read[A, B](handler: Handler[A, B], forward: Boolean) extends Message

case class Deserialize[A, B](data: A, handler: BulkHandler[A, B]) extends Message {
  val forward = true
  def parse: B = handler.parse(data)
}

case class Work[A, B](command: Command[A, B])
