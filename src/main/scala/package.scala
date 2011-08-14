package net.fyrie

import akka.util.ByteString
import akka.dispatch.{ Future, Promise }
import redis.serialization.Parse

package object redis extends ParseResult {
  implicit def doubleToRedisScore(value: Double): RedisScore = InclusiveScore(value)
}

package redis {

  object RedisScore {
    val default: RedisScore = InclusiveScore(1.0)
    val max: RedisScore = InclusiveScore(Double.PositiveInfinity)
    val min: RedisScore = InclusiveScore(Double.NegativeInfinity)
  }
  sealed trait RedisScore {
    def value: Double
    def inclusive: InclusiveScore
    def exclusive: ExclusiveScore
  }
  case class InclusiveScore(value: Double) extends RedisScore {
    def inclusive = this
    def exclusive = ExclusiveScore(value)
  }
  case class ExclusiveScore(value: Double) extends RedisScore {
    def inclusive = InclusiveScore(value)
    def exclusive = this
  }

  sealed trait RedisLimit
  case class Limit(offset: Int, count: Int) extends RedisLimit
  case object NoLimit extends RedisLimit

  sealed trait SortOrder
  object SortOrder {
    case object Asc extends SortOrder
    case object Desc extends SortOrder
  }

  sealed trait Aggregate
  object Aggregate {
    case object Sum extends Aggregate
    case object Min extends Aggregate
    case object Max extends Aggregate
  }

  private[redis] class BoxRedisFloat
}
