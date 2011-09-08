package net.fyrie.redis.types

import akka.util.ByteString

private[redis] sealed trait RedisType

private[redis] case class RedisString(value: String) extends RedisType

private[redis] case class RedisInteger(value: Long) extends RedisType

private[redis] object RedisBulk {
  val notfound = new RedisBulk(None)
  val empty = new RedisBulk(Some(ByteString.empty))
}

private[redis] case class RedisBulk(value: Option[ByteString]) extends RedisType

private[redis] object RedisMulti {
  val notfound = new RedisMulti(None)
  val empty = new RedisMulti(Some(Nil))
}

private[redis] case class RedisMulti(value: Option[List[RedisType]]) extends RedisType

private[redis] case class RedisError(value: String) extends RedisType
