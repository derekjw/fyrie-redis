package net.fyrie.redis
package commands

import serialization._
import akka.util.ByteString

private[redis] trait Servers[Result[_]] {
  this: Commands[Result] ⇒
  import protocol.Constants._

  def save(): Result[Unit] = send(SAVE :: Nil)

  def bgsave(): Result[Unit] = send(BGSAVE :: Nil)

  def lastsave(): Result[Int] = send(LASTSAVE :: Nil)

  //def shutdown(): Result[Nothing]

  def bgrewriteaof(): Result[Unit] = send(BGREWRITEAOF :: Nil)

  def info(): Result[String] = send(INFO :: Nil)

  //def monitor extends Command(OkStatus)
  /*
  def slaveof(hostPort: Option[(String, Int)])(implicit format: Format) extends Command(OkStatus) {
    override def args = hostPort map (x => Iterator(x._1, x._2)) getOrElse Iterator("NO ONE")
  }

  object config {

    def get(param: Any)(implicit format: Format, parse: Parse[A]) extends Command(MultiBulk[A]()(implicitly, parse.manifest)) {
      override def name = "CONFIG"
      override def args = Iterator("GET", param)
    }

    def set(param: Any, value: Any)(implicit format: Format) extends Command(OkStatus) {
      override def name = "CONFIG"
      override def args = Iterator("SET", param, value)
    }

  }
*/
}
