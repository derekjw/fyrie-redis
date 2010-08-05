package net.fyrie
package redis
package akka
package messages

import com.redis.{Reply}
import com.redis.replies.{ReplyProxy}
import com.redis.commands.{Command}

trait Message {
  val forward: Boolean
}

case class Request(command: Command[_], forward: Boolean) extends Message

case class Prepare(command: Command[_], forward: Boolean) extends Message

case class Write(bytes: Array[Byte], replyHandler: Reply[_], forward: Boolean) extends Message

case class Read(replyHandler: Reply[_], forward: Boolean) extends Message

case class Transform[T, U](data: U, replyProxy: ReplyProxy[T,U], forward: Boolean) extends Message {
  def execute: T = replyProxy transform data
}
