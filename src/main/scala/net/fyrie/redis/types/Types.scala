package net.fyrie.redis.types

import akka.util.ByteString

sealed trait RedisType

case class RedisString(value: String) extends RedisType

case class RedisInteger(value: Long) extends RedisType

object RedisBulk {
  val notfound = new RedisBulk(None)
  val empty = new RedisBulk(Some(ByteString.empty))
}

case class RedisBulk(value: Option[ByteString]) extends RedisType

object RedisMulti {
  val notfound = new RedisMulti(None)
  val empty = new RedisMulti(Some(Nil))
}

case class RedisMulti(value: Option[List[RedisType]]) extends RedisType

case class RedisError(value: String) extends RedisType
