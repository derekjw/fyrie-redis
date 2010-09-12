package net.fyrie.redis
package commands

import replies._

case class hset(key: Any, field: Any, value: Any) extends Command[IntAsBoolean]

case class hsetnx(key: Any, field: Any, value: Any) extends Command[IntAsBoolean]

case class hmset(key : Any, fvs: Iterable[(Any,Any)]) extends Command[OkStatus]

case class hget(key : Any, field : Any) extends Command[Bulk]

case class hmget(key : Any, fields : Seq[Any]) extends Command[MultiBulk]

case class hkeys(key : Any) extends Command[MultiBulkAsSet]

case class hvals(key : Any) extends Command[MultiBulkAsFlat]

case class hgetall(key : Any) extends Command[MultiBulkAsMap]

case class hincrby(key : Any, field : Any, value : Long = 1) extends Command[Long]

case class hexists(key : Any, field : Any) extends Command[IntAsBoolean]

case class hdel(key : Any, field : Any) extends Command[IntAsBoolean]

case class hlen(key : Any) extends Command[Long]
