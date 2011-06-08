/*package net.fyrie.redis
package commands

import Command._
import handlers._
import serialization._

trait HashCommands {
  case class hset(key: Any, field: Any, value: Any)(implicit format: Format) extends Command(IntAsBoolean)

  case class hsetnx(key: Any, field: Any, value: Any)(implicit format: Format) extends Command(IntAsBoolean)

  case class hmset(key : Any, fvs: Iterable[Product2[Any,Any]])(implicit format: Format) extends Command(OkStatus) {
    override def args = arg1(key) ++ argN2(fvs)
  }

  case class hget[A](key : Any, field : Any)(implicit format: Format, parse: Parse[A]) extends Command(Bulk[A]()(implicitly, parse.manifest))

  case class hmget[A](key : Any, fields : Seq[Any])(implicit format: Format, parse: Parse[A]) extends Command(MultiBulk[A]()(implicitly, parse.manifest)) {
    override def args = arg1(key) ++ fields.iterator
  }

  case class hkeys[A](key : Any)(implicit format: Format, parse: Parse[A]) extends Command(MultiBulkAsFlat[A]()(implicitly, parse.manifest))

  case class hvals[A](key : Any)(implicit format: Format, parse: Parse[A]) extends Command(MultiBulkAsFlat[A]()(implicitly, parse.manifest))

  case class hgetall[K,V](key : Any)(implicit format: Format, parseK: Parse[K], parseV: Parse[V]) extends Command(MultiBulkAsPairs[K,V]()(implicitly, parseK.manifest, implicitly, parseV.manifest))

  case class hincrby(key : Any, field : Any, value : Long = 1)(implicit format: Format) extends Command(LongInt)

  case class hexists(key : Any, field : Any)(implicit format: Format) extends Command(IntAsBoolean)

  case class hdel(key : Any, field : Any)(implicit format: Format) extends Command(IntAsBoolean)

  case class hlen(key : Any)(implicit format: Format) extends Command(LongInt)
}
*/
