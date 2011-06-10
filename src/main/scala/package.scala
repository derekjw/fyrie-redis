package net.fyrie

import akka.util.ByteString
import akka.dispatch.{Future, Promise}
import redis.serialization.Parse

package object redis {
  implicit def doubleToRedisScore(value: Double): RedisScore = InclusiveScore(value)

  implicit def parseBulkFuture(future: Future[Option[ByteString]]) = new ParseBulkFuture(future)
  implicit def parseMultiBulkFuture(future: Future[Option[List[Option[ByteString]]]]) = new ParseMultiBulkFuture(future)
  implicit def parseMultiBulkListFuture(future: Future[List[Option[ByteString]]]) = new ParseMultiBulkListFuture(future)
  implicit def parseMultiBulkFlatFuture(future: Future[Option[List[ByteString]]]) = new ParseMultiBulkFlatFuture(future)
  implicit def parseMultiBulkFlatListFuture(future: Future[List[ByteString]]) = new ParseMultiBulkFlatListFuture(future)
  implicit def parseMultiBulkSetFuture(future: Future[Set[ByteString]]) = new ParseMultiBulkSetFuture(future)
  implicit def parseMultiBulkMapFuture(future: Future[Map[ByteString, ByteString]]) = new ParseMultiBulkMapFuture(future)
  implicit def parseMultiBulkScoredFuture(future: Future[List[(ByteString, Double)]]) = new ParseMultiBulkScoredFuture(future)

  implicit def parseBulk(value: Option[ByteString]) = new ParseBulk(value)
  implicit def parseMultiBulk(value: Option[List[Option[ByteString]]]) = new ParseMultiBulk(value)
  implicit def parseMultiBulkList(value: List[Option[ByteString]]) = new ParseMultiBulkList(value)
  implicit def parseMultiBulkFlat(value: Option[List[ByteString]]) = new ParseMultiBulkFlat(value)
  implicit def parseMultiBulkFlatList(value: List[ByteString]) = new ParseMultiBulkFlatList(value)
  implicit def parseMultiBulkSet(value: Set[ByteString]) = new ParseMultiBulkSet(value)
  implicit def parseMultiBulkMap(value: Map[ByteString, ByteString]) = new ParseMultiBulkMap(value)
  implicit def parseMultiBulkScored(value: List[(ByteString, Double)]) = new ParseMultiBulkScored(value)

  implicit def parseBulkQueued(queued: RedisClientMulti#Queued[Option[ByteString]]) = new ParseBulkQueued(queued)
  implicit def parseMultiBulkQueued(queued: RedisClientMulti#Queued[Option[List[Option[ByteString]]]]) = new ParseMultiBulkQueued(queued)
  implicit def parseMultiBulkListQueued(queued: RedisClientMulti#Queued[List[Option[ByteString]]]) = new ParseMultiBulkListQueued(queued)
  implicit def parseMultiBulkFlatQueued(queued: RedisClientMulti#Queued[Option[List[ByteString]]]) = new ParseMultiBulkFlatQueued(queued)
  implicit def parseMultiBulkFlatListQueued(queued: RedisClientMulti#Queued[List[ByteString]]) = new ParseMultiBulkFlatListQueued(queued)
  implicit def parseMultiBulkSetQueued(queued: RedisClientMulti#Queued[Set[ByteString]]) = new ParseMultiBulkSetQueued(queued)
  implicit def parseMultiBulkMapQueued(queued: RedisClientMulti#Queued[Map[ByteString, ByteString]]) = new ParseMultiBulkMapQueued(queued)
  implicit def parseMultiBulkScoredQueued(queued: RedisClientMulti#Queued[List[(ByteString, Double)]]) = new ParseMultiBulkScoredQueued(queued)

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

  private[redis] class ParseBulkFuture(future: Future[Option[ByteString]]) {
    def parse[A: Parse]: Future[Option[A]] = future.map(_.map(Parse(_)))
  }
  private[redis] class ParseMultiBulkFuture(future: Future[Option[List[Option[ByteString]]]]) {
    def parse[A: Parse]: Future[Option[List[Option[A]]]] = future.map(_.map(_.map(_.map(Parse(_)))))
  }
  private[redis] class ParseMultiBulkListFuture(future: Future[List[Option[ByteString]]]) {
    def parse[A: Parse]: Future[List[Option[A]]] = future.map(_.map(_.map(Parse(_))))
  }
  private[redis] class ParseMultiBulkFlatFuture(future: Future[Option[List[ByteString]]]) {
    def parse[A: Parse]: Future[Option[List[A]]] = future.map(_.map(_.map(Parse(_))))
  }
  private[redis] class ParseMultiBulkFlatListFuture(future: Future[List[ByteString]]) {
    def parse[A: Parse]: Future[List[A]] = future.map(_.map(Parse(_)))
  }
  private[redis] class ParseMultiBulkSetFuture(future: Future[Set[ByteString]]) {
    def parse[A: Parse]: Future[Set[A]] = future.map(_.map(Parse(_)))
  }
  private[redis] class ParseMultiBulkMapFuture(future: Future[Map[ByteString, ByteString]]) {
    def parse[K: Parse, V: Parse]: Future[Map[K, V]] = future.map(_.map(kv => (Parse[K](kv._1), Parse[V](kv._2))))
  }
  private[redis] class ParseMultiBulkScoredFuture(future: Future[List[(ByteString, Double)]]) {
    def parse[A: Parse]: Future[List[(A, Double)]] = future.map(_.map(kv => (Parse(kv._1), kv._2)))
  }

  private[redis] class ParseBulk(value: Option[ByteString]) {
    def parse[A: Parse]: Option[A] = value.map(Parse(_))
  }
  private[redis] class ParseMultiBulk(value: Option[List[Option[ByteString]]]) {
    def parse[A: Parse]: Option[List[Option[A]]] = value.map(_.map(_.map(Parse(_))))
  }
  private[redis] class ParseMultiBulkList(value: List[Option[ByteString]]) {
    def parse[A: Parse]: List[Option[A]] = value.map(_.map(Parse(_)))
  }
  private[redis] class ParseMultiBulkFlat(value: Option[List[ByteString]]) {
    def parse[A: Parse]: Option[List[A]] = value.map(_.map(Parse(_)))
  }
  private[redis] class ParseMultiBulkFlatList(value: List[ByteString]) {
    def parse[A: Parse]: List[A] = value.map(Parse(_))
  }
  private[redis] class ParseMultiBulkSet(value: Set[ByteString]) {
    def parse[A: Parse]: Set[A] = value.map(Parse(_))
  }
  private[redis] class ParseMultiBulkMap(value: Map[ByteString, ByteString]) {
    def parse[K: Parse, V: Parse]: Map[K, V] = value.map(kv => (Parse[K](kv._1), Parse[V](kv._2)))
  }
  private[redis] class ParseMultiBulkScored(value: List[(ByteString, Double)]) {
    def parse[A: Parse]: List[(A, Double)] = value.map(kv => (Parse(kv._1), kv._2))
  }

  private[redis] class ParseBulkQueued(queued: RedisClientMulti#Queued[Option[ByteString]]) {
    def parse[A: Parse]: RedisClientMulti#Queued[Option[A]] = queued.map(_.map(Parse(_)))
  }
  private[redis] class ParseMultiBulkQueued(queued: RedisClientMulti#Queued[Option[List[Option[ByteString]]]]) {
    def parse[A: Parse]: RedisClientMulti#Queued[Option[List[Option[A]]]] = queued.map(_.map(_.map(_.map(Parse(_)))))
  }
  private[redis] class ParseMultiBulkListQueued(queued: RedisClientMulti#Queued[List[Option[ByteString]]]) {
    def parse[A: Parse]: RedisClientMulti#Queued[List[Option[A]]] = queued.map(_.map(_.map(Parse(_))))
  }
  private[redis] class ParseMultiBulkFlatQueued(queued: RedisClientMulti#Queued[Option[List[ByteString]]]) {
    def parse[A: Parse]: RedisClientMulti#Queued[Option[List[A]]] = queued.map(_.map(_.map(Parse(_))))
  }
  private[redis] class ParseMultiBulkFlatListQueued(queued: RedisClientMulti#Queued[List[ByteString]]) {
    def parse[A: Parse]: RedisClientMulti#Queued[List[A]] = queued.map(_.map(Parse(_)))
  }
  private[redis] class ParseMultiBulkSetQueued(queued: RedisClientMulti#Queued[Set[ByteString]]) {
    def parse[A: Parse]: RedisClientMulti#Queued[Set[A]] = queued.map(_.map(Parse(_)))
  }
  private[redis] class ParseMultiBulkMapQueued(queued: RedisClientMulti#Queued[Map[ByteString, ByteString]]) {
    def parse[K: Parse, V: Parse]: RedisClientMulti#Queued[Map[K, V]] = queued.map(_.map(kv => (Parse[K](kv._1), Parse[V](kv._2))))
  }
  private[redis] class ParseMultiBulkScoredQueued(queued: RedisClientMulti#Queued[List[(ByteString, Double)]]) {
    def parse[A: Parse]: RedisClientMulti#Queued[List[(A, Double)]] = queued.map(_.map(kv => (Parse(kv._1), kv._2)))
  }
}
