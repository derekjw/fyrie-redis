package net.fyrie.redis
package commands

import replies._
import Helpers._

// ZADD
// Add the specified member having the specified score to the sorted set stored at key.
case class zadd(key: Array[Byte], score: Double, member: Array[Byte]) extends Command[IntAsBoolean]

// ZREM
// Remove the specified member from the sorted set value stored at key.
case class zrem(key: Array[Byte], member: Array[Byte]) extends Command[Int]

// ZINCRBY
// 
case class zincrby(key: Array[Byte], incr: Double, member: Array[Byte]) extends Command[Bulk]

// ZCARD
// 
case class zcard(key: Array[Byte]) extends Command[Int]

// ZSCORE
// 
case class zscore(key: Array[Byte], element: Array[Byte]) extends Command[Bulk]

// ZRANGE
// 

case class zrange(key: Array[Byte], start: Int = 0, end: Int = -1) extends Command[MultiBulkAsFlat]

case class zrangeWithScores(key: Array[Byte], start: Int = 0, end: Int = -1) extends Command[MultiBulkWithScores] {
  override def name = "ZRANGE".getBytes
  override def args = super.args :+ "WITHSCORES".getBytes
}

case class zrevrange(key: Array[Byte], start: Int = 0, end: Int = -1) extends Command[MultiBulkAsFlat]

case class zrevrangeWithScores(key: Array[Byte], start: Int = 0, end: Int = -1) extends Command[MultiBulkWithScores] {
  override def name = "ZREVRANGE".getBytes
  override def args = super.args :+ "WITHSCORES".getBytes
}

// ZRANGEBYSCORE
// 
case class zrangebyscore(key: Array[Byte], min: Double = Double.MinValue, minInclusive: Boolean = true, max: Double = Double.MaxValue, maxInclusive: Boolean = true, limit: Option[(Int, Int)] = None) extends Command[MultiBulk] {
  override def args = Seq(key, getBytes(min, minInclusive), getBytes(max, maxInclusive)) ++ limit.toList.map(x => List("LIMIT".getBytes, getBytes(x._1), getBytes(x._2))).flatten
}

case class zrangebyscoreWithScores(key: Array[Byte], min: Double = Double.MinValue, minInclusive: Boolean = true, max: Double = Double.MaxValue, maxInclusive: Boolean = true, limit: Option[(Int, Int)] = None) extends Command[MultiBulk] {
  override def name = "ZRANGEBYSCORE".getBytes
  override def args = Seq(key, getBytes(min, minInclusive), getBytes(max, maxInclusive)) ++ limit.toList.map(x => List("LIMIT".getBytes, getBytes(x._1), getBytes(x._2))).flatten :+ "WITHSCORES".getBytes
}

case class zcount(key: Array[Byte], min: Double = Double.MinValue, minInclusive: Boolean = true, max: Double = Double.MaxValue, maxInclusive: Boolean = true) extends Command[Int] {
  override def args = Seq(key, getBytes(min, minInclusive), getBytes(max, maxInclusive))
}

// ZRANK
// ZREVRANK
//
case class zrank(key: Array[Byte], member: Array[Byte]) extends Command[Int]

case class zrevrank(key: Array[Byte], member: Array[Byte]) extends Command[Int]

// ZREMRANGEBYRANK
//
case class zremrangebyrank(key: Array[Byte], start: Int = 0, end: Int = -1) extends Command[Int]

// ZREMRANGEBYSCORE
//
case class zremrangebyscore(key: Array[Byte], start: Double, end: Double) extends Command[Int]

// ZUNION
//
case class zunionstore(dstKey: Array[Byte], keys: Iterable[Array[Byte]], aggregate: Option[AggregateScore] = None) extends Command[Int] {
  override def args = Seq(dstKey, getBytes(keys.size)) ++ keys ++ aggregate.toList.flatMap(x => List("AGGREGATE".getBytes, x.getBytes))
}

case class zunionstoreWeighted(dstKey: Array[Byte], kws: Iterable[(Array[Byte], Double)], aggregate: Option[AggregateScore] = None) extends Command[Int] {
  override def name = "ZUNIONSTORE".getBytes
  override def args = {
    val (ks, ws) = kws.unzip
    Seq(Seq(dstKey), Seq(getBytes(kws.size)), ks.toSeq, Seq("WEIGHTS".getBytes), ws.map(getBytes).toSeq, aggregate.toList.flatMap(x => List("AGGREGATE".getBytes, x.getBytes))).flatten
  }
}

