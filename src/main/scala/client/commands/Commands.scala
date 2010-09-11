package net.fyrie.redis
package commands

import replies._
import Helpers._
import scala.collection.mutable.ArrayBuilder

case class keys(pattern: Array[Byte] = "*".getBytes) extends Command[MultiBulk]

case object randomkey extends Command[Bulk]

case class rename(oldkey: Array[Byte], newkey: Array[Byte]) extends Command[OkStatus]

case class renamenx(oldkey: Array[Byte], newkey: Array[Byte]) extends Command[IntAsBoolean]

case object dbsize extends Command[Int]

case class exists(key: Array[Byte]) extends Command[IntAsBoolean]

case class del(keys: Iterable[Array[Byte]]) extends Command[Int] {
  override def args = keys.toSeq
}

case class getType(key: Array[Byte]) extends Command[Status] {
  override def name = "TYPE".getBytes
}

case class expire(key: Array[Byte], seconds: Int) extends Command[IntAsBoolean]

case class expireat(key: Array[Byte], unixtime: Int) extends Command[IntAsBoolean]

case class select(index: Int = 0) extends Command[OkStatus]

case object flushdb extends Command[OkStatus]

case object flushall extends Command[OkStatus]

case class move(key: Array[Byte], db: Int = 0) extends Command[IntAsBoolean]

case object quit extends Command[Nothing]

case class auth(secret: Array[Byte]) extends Command[OkStatus]

case class multiexec(commandList: Seq[Command[_]]) extends Command[MultiExec]()(new MultiExecReply(commandList.map(_.replyHandler))) {
  override def toBytes = {
    val b = new ArrayBuilder.ofByte
    b ++= Helpers.cmd(Seq("MULTI".getBytes))
    commandList.foreach(b ++= _.toBytes)
    b ++= Helpers.cmd(Seq("EXEC".getBytes))
    b.result
  }
}

case class sort(key: Array[Byte], by: Option[Array[Byte]] = None, limit: Option[(Int, Int)] = None, get: Seq[Array[Byte]] = Nil, order: Option[SortOrder] = None, alpha: Boolean = false, store: Option[Array[Byte]] = None) extends Command[MultiBulk] {
  override def args = getBytesSeq(Seq(key,
                                      by.map(Seq("BY", _)),
                                      limit.map{case (start, count) => Seq("LIMIT", start, count)},
                                      get.map(Seq("GET", _)),
                                      order,
                                      if (alpha) (Some("ALPHA".getBytes)) else (None),
                                      store.map(Seq("STORE", _))))
}
