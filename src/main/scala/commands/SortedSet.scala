/*package net.fyrie.redis
package commands

import handlers._
import serialization._
import Command._

trait SortedSetCommands {
  // ZADD
  // Add the specified member having the specified score to the sorted set stored at key.
  case class zadd(key: Any, score: Double, member: Any)(implicit format: Format) extends Command(IntAsBoolean)

  // ZREM
  // Remove the specified member from the sorted set value stored at key.
  case class zrem(key: Any, member: Any)(implicit format: Format) extends Command(ShortInt)

  // ZINCRBY
  //
  case class zincrby[A](key: Any, incr: Double, member: Any)(implicit format: Format, parse: Parse[A]) extends Command(Bulk[A]()(implicitly, parse.manifest))

  // ZCARD
  //
  case class zcard(key: Any)(implicit format: Format) extends Command(ShortInt)

  // ZSCORE
  //
  case class zscore[A](key: Any, element: Any)(implicit format: Format, parse: Parse[A]) extends Command(Bulk[A]()(implicitly, parse.manifest))

  // ZRANGE
  //

  case class zrange[A](key: Any, start: Int = 0, end: Int = -1)(implicit format: Format, parse: Parse[A]) extends Command(MultiBulkAsFlat[A]()(implicitly, parse.manifest))

  case class zrangeWithScores[A](key: Any, start: Int = 0, end: Int = -1)(implicit format: Format, parse: Parse[A]) extends Command(MultiBulkWithScores[A]()(implicitly, parse.manifest)) {
    override def name = "ZRANGE"
    override def args = super.args ++ arg1("WITHSCORES")
  }

  case class zrevrange[A](key: Any, start: Int = 0, end: Int = -1)(implicit format: Format, parse: Parse[A]) extends Command(MultiBulkAsFlat[A]()(implicitly, parse.manifest))

  case class zrevrangeWithScores[A](key: Any, start: Int = 0, end: Int = -1)(implicit format: Format, parse: Parse[A]) extends Command(MultiBulkWithScores[A]()(implicitly, parse.manifest)) {
    override def name = "ZREVRANGE"
    override def args = super.args ++ arg1("WITHSCORES")
  }

  // ZRANGEBYSCORE
  //
  case class zrangebyscore[A](key: Any, min: Double = Double.MinValue, minInclusive: Boolean = true, max: Double = Double.MaxValue, maxInclusive: Boolean = true, limit: Option[(Int, Int)] = None)(implicit format: Format, parse: Parse[A]) extends Command(MultiBulkAsFlat[A]()(implicitly, parse.manifest)) {
    override def args = Iterator(key, serializeDouble(min, minInclusive), serializeDouble(max, maxInclusive)) ++ argN2("LIMIT", limit)
  }

  case class zrangebyscoreWithScores[A](key: Any, min: Double = Double.MinValue, minInclusive: Boolean = true, max: Double = Double.MaxValue, maxInclusive: Boolean = true, limit: Option[(Int, Int)] = None)(implicit format: Format, parse: Parse[A]) extends Command(MultiBulkWithScores[A]()(implicitly, parse.manifest)) {
    override def name = "ZRANGEBYSCORE"
    override def args = Iterator(key, serializeDouble(min, minInclusive), serializeDouble(max, maxInclusive)) ++ argN2("LIMIT", limit) ++ arg1("WITHSCORES")
  }

  case class zcount(key: Any, min: Double = Double.MinValue, minInclusive: Boolean = true, max: Double = Double.MaxValue, maxInclusive: Boolean = true)(implicit format: Format) extends Command(ShortInt) {
    override def args = Iterator(key, serializeDouble(min, minInclusive), serializeDouble(max, maxInclusive))
  }

  // ZRANK
  // ZREVRANK
  //
  case class zrank(key: Any, member: Any)(implicit format: Format) extends Command(ShortInt)

  case class zrevrank(key: Any, member: Any)(implicit format: Format) extends Command(ShortInt)

  // ZREMRANGEBYRANK
  //
  case class zremrangebyrank(key: Any, start: Int = 0, end: Int = -1)(implicit format: Format) extends Command(ShortInt)

  // ZREMRANGEBYSCORE
  //
  case class zremrangebyscore(key: Any, start: Double, end: Double)(implicit format: Format) extends Command(ShortInt)

  // ZUNION
  //
  case class zunionstore(dstKey: Any, keys: Iterable[Any], aggregate: Option[AggregateScore] = None)(implicit format: Format) extends Command(ShortInt) {
    override def args = Iterator(dstKey, keys.size) ++ keys.iterator ++ argN1("AGGREGATE", aggregate)
  }

  case class zunionstoreWeighted(dstKey: Any, kws: Iterable[Product2[Any, Double]], aggregate: Option[AggregateScore] = None)(implicit format: Format) extends Command(ShortInt) {
    override def name = "ZUNIONSTORE"
    override def args = Iterator(dstKey, kws.size) ++ kws.iterator.map(_._1) ++ arg1("WEIGHTS") ++ kws.iterator.map(_._2) ++ argN1("AGGREGATE", aggregate)
  }
}
*/
