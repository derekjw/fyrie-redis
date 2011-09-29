package net.fyrie.redis
package commands

import serialization._
import akka.util.ByteString

private[redis] trait PubSub[Result[_]] {
  this: Commands[Result] ⇒
  import protocol.Constants._

  def publish[A: Store, B: Store](channel: A, message: B): Result[Int] = send(PUBLISH :: Store(channel) :: Store(message) :: Nil)

}
