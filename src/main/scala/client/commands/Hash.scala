package net.fyrie.redis
package commands

import replies._

case class hset(key: Array[Byte], field: Array[Byte], value: Array[Byte]) extends Command[IntAsBoolean]

case class hsetnx(key: Array[Byte], field: Array[Byte], value: Array[Byte]) extends Command[IntAsBoolean]

case class hmset(key : Array[Byte], fvs: Iterable[(Array[Byte],Array[Byte])]) extends Command[OkStatus] {
  override def args = key +: fvs.toSeq.flatMap{ case (f,v) => Seq(f,v) }
}

case class hget(key : Array[Byte], field : Array[Byte]) extends Command[Bulk]

case class hmget(key : Array[Byte], fields : Iterable[Array[Byte]]) extends Command[MultiBulk] {
  override def args = key +: fields.toSeq
}

case class hkeys(key : Array[Byte]) extends Command[MultiBulkAsSet]

case class hvals(key : Array[Byte]) extends Command[MultiBulkAsFlat]

case class hgetall(key : Array[Byte]) extends Command[MultiBulkAsMap]

case class hincrby(key : Array[Byte], field : Array[Byte], value : Long = 1) extends Command[Long]

case class hexists(key : Array[Byte], field : Array[Byte]) extends Command[IntAsBoolean]

case class hdel(key : Array[Byte], field : Array[Byte]) extends Command[IntAsBoolean]

case class hlen(key : Array[Byte]) extends Command[Long]
