package net.fyrie
package redis
package akka
package messages

trait Message {
  val forward: Boolean
}

case class Request[T, V](command: Command[T], forward: Boolean, transform: (T) => V) extends Message

case class Prepare[T, V](command: Command[T], forward: Boolean, transform: (T) => V) extends Message

case class Write[T, V](bytes: Array[Byte], replyHandler: Reply[T], forward: Boolean, transform: (T) => V) extends Message

case class Read[T, V](replyHandler: Reply[T], forward: Boolean, transform: (T) => V) extends Message

case class Transform[T, V](data: T, forward: Boolean, transform: (T) => V) extends Message

case class Work[T,V](command: Command[T], transform: (T) => V)
