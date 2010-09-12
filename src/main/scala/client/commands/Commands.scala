package net.fyrie.redis
package commands

import replies._
import Helpers._
import scala.collection.mutable.ArrayBuilder

case class keys(pattern: Any = "*") extends Command[MultiBulk]

case object randomkey extends Command[Bulk]

case class rename(oldkey: Any, newkey: Any) extends Command[OkStatus]

case class renamenx(oldkey: Any, newkey: Any) extends Command[IntAsBoolean]

case object dbsize extends Command[Int]

case class exists(key: Any) extends Command[IntAsBoolean]

case class del(keys: Iterable[Any]) extends Command[Int]

case class getType(key: Any) extends Command[Status] {
  override def name = "TYPE".getBytes
}

case class expire(key: Any, seconds: Int) extends Command[IntAsBoolean]

case class expireat(key: Any, unixtime: Int) extends Command[IntAsBoolean]

case class select(index: Int = 0) extends Command[OkStatus]

case object flushdb extends Command[OkStatus]

case object flushall extends Command[OkStatus]

case class move(key: Any, db: Int = 0) extends Command[IntAsBoolean]

case object quit extends Command[Nothing]

case class auth(secret: Any) extends Command[OkStatus]

case class multiexec(commandList: Seq[Command[_]]) extends Command[MultiExec]()(new MultiExecReply(commandList.map(_.replyHandler))) {
  override def toBytes = {
    val b = new ArrayBuilder.ofByte
    b ++= Helpers.cmd(Seq("MULTI".getBytes))
    commandList.foreach(b ++= _.toBytes)
    b ++= Helpers.cmd(Seq("EXEC".getBytes))
    b.result
  }
}

case class sort(key: Any, by: Option[Any] = None, limit: Option[(Int, Int)] = None, get: Seq[Any] = Nil, order: Option[SortOrder] = None, alpha: Boolean = false, store: Option[Any] = None) extends Command[MultiBulkAsFlat] {
  override def args = getBytesSeq(Seq(key,
                                      by.map(Seq("BY", _)),
                                      limit.map(("LIMIT", _)),
                                      get.map(Seq("GET", _)),
                                      order,
                                      if (alpha) (Some("ALPHA")) else (None),
                                      store.map(Seq("STORE", _))))
}
