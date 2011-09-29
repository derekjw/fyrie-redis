package net.fyrie.redis
package commands

import serialization._
import akka.util.ByteString

private[redis] trait SortedSets[Result[_]] {
  this: Commands[Result] ⇒
  import protocol.Constants._

  def zadd[K: Store, M: Store](key: K, member: M, score: Double = 1.0): Result[Boolean] =
    send(ZADD :: Store(key) :: Store(score) :: Store(member) :: Nil)

  def zcard[K: Store](key: K): Result[Int] =
    send(ZCARD :: Store(key) :: Nil)

  def zcount[K: Store](key: K, min: RedisScore = RedisScore.min, max: RedisScore = RedisScore.max): Result[Int] =
    send(ZCOUNT :: Store(key) :: Store(min) :: Store(max) :: Nil)

  def zincrby[K: Store, M: Store](key: K, member: M, incr: Double = 1.0): Result[Double] =
    send(ZINCRBY :: Store(key) :: Store(incr) :: Store(member) :: Nil)

  def zinterstore[D: Store, K: Store](dstKey: D, keys: Iterable[K], aggregate: Aggregate = Aggregate.Sum): Result[Int] = {
    var cmd = AGGREGATE :: Store(aggregate) :: Nil
    var keyCount = 0
    keys foreach { k ⇒
      cmd ::= Store(k)
      keyCount += 1
    }
    send(ZINTERSTORE :: Store(dstKey) :: Store(keyCount) :: cmd)
  }

  def zinterstoreWeighted[D: Store, K: Store](dstKey: D, kws: Iterable[Product2[K, Double]], aggregate: Aggregate = Aggregate.Sum): Result[Int] = {
    var cmd: List[ByteString] = Nil
    var keyCount = 0
    kws foreach { kw ⇒
      cmd ::= Store(kw._1)
      keyCount += 1
    }
    cmd ::= WEIGHTS
    kws foreach { kw ⇒ cmd ::= Store(kw._2) }
    send(ZINTERSTORE :: Store(dstKey) :: Store(keyCount) :: (Store(aggregate) :: AGGREGATE :: cmd).reverse)
  }

  def zrange[K: Store](key: K, start: Int = 0, stop: Int = -1): Result[List[ByteString]] =
    send(ZRANGE :: Store(key) :: Store(start) :: Store(stop) :: Nil)

  def zrangeWithScores[K: Store](key: K, start: Int = 0, stop: Int = -1): Result[List[(ByteString, Double)]] =
    send(ZRANGE :: Store(key) :: Store(start) :: Store(stop) :: WITHSCORES :: Nil)

  def zrangebyscore[K: Store](key: K, min: RedisScore = RedisScore.min, max: RedisScore = RedisScore.max, limit: RedisLimit = NoLimit): Result[List[ByteString]] =
    send(ZRANGEBYSCORE :: Store(key) :: Store(min) :: Store(max) :: (limit match {
      case NoLimit     ⇒ Nil
      case Limit(o, c) ⇒ LIMIT :: Store(o) :: Store(c) :: Nil
    }))

  def zrangebyscoreWithScores[K: Store](key: K, min: RedisScore = RedisScore.min, max: RedisScore = RedisScore.max, limit: RedisLimit = NoLimit): Result[List[(ByteString, Double)]] =
    send(ZRANGEBYSCORE :: Store(key) :: Store(min) :: Store(max) :: WITHSCORES :: (limit match {
      case NoLimit     ⇒ Nil
      case Limit(o, c) ⇒ LIMIT :: Store(o) :: Store(c) :: Nil
    }))

  def zrank[K: Store, M: Store](key: K, member: M): Result[Option[Int]] =
    send(ZRANK :: Store(key) :: Store(member) :: Nil)

  def zrem[K: Store, M: Store](key: K, member: M): Result[Boolean] =
    send(ZREM :: Store(key) :: Store(member) :: Nil)

  def zremrangebyrank[K: Store](key: K, start: Int = 0, stop: Int = -1): Result[Int] =
    send(ZREMRANGEBYRANK :: Store(key) :: Store(start) :: Store(stop) :: Nil)

  def zremrangebyscore[K: Store](key: K, min: RedisScore = RedisScore.min, max: RedisScore = RedisScore.max): Result[Int] =
    send(ZREMRANGEBYSCORE :: Store(key) :: Store(min) :: Store(max) :: Nil)

  def zrevrange[K: Store](key: K, start: Int = 0, stop: Int = -1): Result[List[ByteString]] =
    send(ZREVRANGE :: Store(key) :: Store(start) :: Store(stop) :: Nil)

  def zrevrangeWithScores[K: Store](key: K, start: Int = 0, stop: Int = -1): Result[List[(ByteString, Double)]] =
    send(ZREVRANGE :: Store(key) :: Store(start) :: Store(stop) :: WITHSCORES :: Nil)

  def zrevrangebyscore[K: Store](key: K, min: RedisScore = RedisScore.min, max: RedisScore = RedisScore.max, limit: RedisLimit = NoLimit): Result[List[ByteString]] =
    send(ZREVRANGEBYSCORE :: Store(key) :: Store(min) :: Store(max) :: (limit match {
      case NoLimit     ⇒ Nil
      case Limit(o, c) ⇒ LIMIT :: Store(o) :: Store(c) :: Nil
    }))

  def zrevrangebyscoreWithScores[K: Store](key: K, min: RedisScore = RedisScore.min, max: RedisScore = RedisScore.max, limit: RedisLimit = NoLimit): Result[List[(ByteString, Double)]] =
    send(ZREVRANGEBYSCORE :: Store(key) :: Store(min) :: Store(max) :: WITHSCORES :: (limit match {
      case NoLimit     ⇒ Nil
      case Limit(o, c) ⇒ LIMIT :: Store(o) :: Store(c) :: Nil
    }))

  def zrevrank[K: Store, M: Store](key: K, member: M): Result[Option[Int]] =
    send(ZREVRANK :: Store(key) :: Store(member) :: Nil)

  def zscore[K: Store, M: Store](key: K, member: M): Result[Option[Double]] =
    send(ZSCORE :: Store(key) :: Store(member) :: Nil)

  def zunionstore[D: Store, K: Store](dstKey: D, keys: Iterable[K], aggregate: Aggregate = Aggregate.Sum): Result[Int] = {
    var cmd = AGGREGATE :: Store(aggregate) :: Nil
    var keyCount = 0
    keys foreach { k ⇒
      cmd ::= Store(k)
      keyCount += 1
    }
    send(ZUNIONSTORE :: Store(dstKey) :: Store(keyCount) :: cmd)
  }

  def zunionstoreWeighted[D: Store, K: Store](dstKey: D, kws: Iterable[Product2[K, Double]], aggregate: Aggregate = Aggregate.Sum): Result[Int] = {
    var cmd: List[ByteString] = Nil
    var keyCount = 0
    kws foreach { kw ⇒
      cmd ::= Store(kw._1)
      keyCount += 1
    }
    cmd ::= WEIGHTS
    kws foreach { kw ⇒ cmd ::= Store(kw._2) }
    send(ZUNIONSTORE :: Store(dstKey) :: Store(keyCount) :: (Store(aggregate) :: AGGREGATE :: cmd).reverse)
  }
}
