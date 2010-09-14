package net.fyrie.redis
package commands

import helpers._
import replies._

// ZADD
// Add the specified member having the specified score to the sorted set stored at key.
case class zadd(key: Any, score: Double, member: Any) extends Command[IntAsBoolean]

// ZREM
// Remove the specified member from the sorted set value stored at key.
case class zrem(key: Any, member: Any) extends Command[Int]

// ZINCRBY
// 
case class zincrby(key: Any, incr: Double, member: Any) extends Command[Bulk]

// ZCARD
// 
case class zcard(key: Any) extends Command[Int]

// ZSCORE
// 
case class zscore(key: Any, element: Any) extends Command[Bulk]

// ZRANGE
// 

case class zrange(key: Any, start: Int = 0, end: Int = -1) extends Command[MultiBulkAsFlat]

case class zrangeWithScores(key: Any, start: Int = 0, end: Int = -1) extends Command[MultiBulkWithScores] {
  override def name = "ZRANGE"
  override def args = super.args ++ arg1("WITHSCORES")
}

case class zrevrange(key: Any, start: Int = 0, end: Int = -1) extends Command[MultiBulkAsFlat]

case class zrevrangeWithScores(key: Any, start: Int = 0, end: Int = -1) extends Command[MultiBulkWithScores] {
  override def name = "ZREVRANGE"
  override def args = super.args ++ arg1("WITHSCORES")
}

// ZRANGEBYSCORE
// 
case class zrangebyscore(key: Any, min: Double = Double.MinValue, minInclusive: Boolean = true, max: Double = Double.MaxValue, maxInclusive: Boolean = true, limit: Option[(Int, Int)] = None) extends Command[MultiBulk] {
  override def args = Iterator(key, Command.serializeDouble(min, minInclusive), Command.serializeDouble(max, maxInclusive)) ++ argN2("LIMIT", limit)
}

case class zrangebyscoreWithScores(key: Any, min: Double = Double.MinValue, minInclusive: Boolean = true, max: Double = Double.MaxValue, maxInclusive: Boolean = true, limit: Option[(Int, Int)] = None) extends Command[MultiBulk] {
  override def name = "ZRANGEBYSCORE"
  override def args = Iterator(key, Command.serializeDouble(min, minInclusive), Command.serializeDouble(max, maxInclusive)) ++ argN2("LIMIT", limit) ++ arg1("WITHSCORES")
}

case class zcount(key: Any, min: Double = Double.MinValue, minInclusive: Boolean = true, max: Double = Double.MaxValue, maxInclusive: Boolean = true) extends Command[Int] {
  override def args = Iterator(key, Command.serializeDouble(min, minInclusive), Command.serializeDouble(max, maxInclusive))
}

// ZRANK
// ZREVRANK
//
case class zrank(key: Any, member: Any) extends Command[Int]

case class zrevrank(key: Any, member: Any) extends Command[Int]

// ZREMRANGEBYRANK
//
case class zremrangebyrank(key: Any, start: Int = 0, end: Int = -1) extends Command[Int]

// ZREMRANGEBYSCORE
//
case class zremrangebyscore(key: Any, start: Double, end: Double) extends Command[Int]

// ZUNION
//
case class zunionstore(dstKey: Any, keys: Iterable[Any], aggregate: Option[AggregateScore] = None) extends Command[Int] {
  override def args = Iterator(dstKey, keys.size) ++ keys.iterator ++ argN1("AGGREGATE", aggregate)
}

case class zunionstoreWeighted(dstKey: Any, kws: Iterable[Product2[Any, Double]], aggregate: Option[AggregateScore] = None) extends Command[Int] {
  override def name = "ZUNIONSTORE"
  override def args = Iterator(dstKey, kws.size) ++ kws.iterator.map(_._1) ++ arg1("WEIGHTS") ++ kws.iterator.map(_._2) ++ argN1("AGGREGATE", aggregate)
}

