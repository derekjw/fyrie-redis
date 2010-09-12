package net.fyrie.redis
package commands

import replies._
import Helpers._

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
  override def name = "ZRANGE".getBytes
  override def args = super.args :+ "WITHSCORES".getBytes
}

case class zrevrange(key: Any, start: Int = 0, end: Int = -1) extends Command[MultiBulkAsFlat]

case class zrevrangeWithScores(key: Any, start: Int = 0, end: Int = -1) extends Command[MultiBulkWithScores] {
  override def name = "ZREVRANGE".getBytes
  override def args = super.args :+ "WITHSCORES".getBytes
}

// ZRANGEBYSCORE
// 
case class zrangebyscore(key: Any, min: Double = Double.MinValue, minInclusive: Boolean = true, max: Double = Double.MaxValue, maxInclusive: Boolean = true, limit: Option[(Int, Int)] = None) extends Command[MultiBulk] {
  override def args = getBytesSeq(Seq(key,
                                      getBytes(min, minInclusive),
                                      getBytes(max, maxInclusive),
                                      limit.map{case (start, count) => Seq("LIMIT", start, count)}))
}

case class zrangebyscoreWithScores(key: Any, min: Double = Double.MinValue, minInclusive: Boolean = true, max: Double = Double.MaxValue, maxInclusive: Boolean = true, limit: Option[(Int, Int)] = None) extends Command[MultiBulk] {
  override def name = "ZRANGEBYSCORE".getBytes
  override def args = getBytesSeq(Seq(key,
                                      getBytes(min, minInclusive),
                                      getBytes(max, maxInclusive),
                                      limit.map{case (start, count) => Seq("LIMIT", start, count)},
                                      "WITHSCORES"))
}

case class zcount(key: Any, min: Double = Double.MinValue, minInclusive: Boolean = true, max: Double = Double.MaxValue, maxInclusive: Boolean = true) extends Command[Int] {
  override def args = getBytesSeq(Seq(key,
                                      getBytes(min, minInclusive),
                                      getBytes(max, maxInclusive)))
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
  override def args = getBytesSeq(Seq(dstKey,
                                     keys.size,
                                     keys,
                                     aggregate.map(x => ("AGGREGATE", x.getBytes))))
}

case class zunionstoreWeighted(dstKey: Any, kws: Iterable[(Any, Double)], aggregate: Option[AggregateScore] = None) extends Command[Int] {
  override def name = "ZUNIONSTORE".getBytes
  override def args = getBytesSeq(Seq(dstKey,
                                      kws.size,
                                      kws.iterator.map(_._1),
                                      "WEIGHTS",
                                      kws.iterator.map(_._2),
                                      aggregate.map(x => ("AGGREGATE", x.getBytes))))
}

