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

  implicit def parseBulkQueued(queued: Queued[Future[Option[ByteString]]]) = new ParseBulkQueued(queued)
  implicit def parseMultiBulkQueued(queued: Queued[Future[Option[List[Option[ByteString]]]]]) = new ParseMultiBulkQueued(queued)
  implicit def parseMultiBulkListQueued(queued: Queued[Future[List[Option[ByteString]]]]) = new ParseMultiBulkListQueued(queued)
  implicit def parseMultiBulkFlatQueued(queued: Queued[Future[Option[List[ByteString]]]]) = new ParseMultiBulkFlatQueued(queued)
  implicit def parseMultiBulkFlatListQueued(queued: Queued[Future[List[ByteString]]]) = new ParseMultiBulkFlatListQueued(queued)
  implicit def parseMultiBulkSetQueued(queued: Queued[Future[Set[ByteString]]]) = new ParseMultiBulkSetQueued(queued)
  implicit def parseMultiBulkMapQueued(queued: Queued[Future[Map[ByteString, ByteString]]]) = new ParseMultiBulkMapQueued(queued)
  implicit def parseMultiBulkScoredQueued(queued: Queued[Future[List[(ByteString, Double)]]]) = new ParseMultiBulkScoredQueued(queued)

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
    def parse[A: Parse, B: Parse] = future.map(_.grouped(2).collect {
      case List(a, b) => (a map (Parse[A](_)), b map (Parse[B](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse] = future.map(_.grouped(3).collect {
      case List(a, b, c) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse] = future.map(_.grouped(4).collect {
      case List(a, b, c, d) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse] = future.map(_.grouped(5).collect {
      case List(a, b, c, d, e) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse] = future.map(_.grouped(6).collect {
      case List(a, b, c, d, e, f) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse] = future.map(_.grouped(7).collect {
      case List(a, b, c, d, e, f, g) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse] = future.map(_.grouped(8).collect {
      case List(a, b, c, d, e, f, g, h) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse] = future.map(_.grouped(9).collect {
      case List(a, b, c, d, e, f, g, h, i) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse] = future.map(_.grouped(10).collect {
      case List(a, b, c, d, e, f, g, h, i, j) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse] = future.map(_.grouped(11).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse] = future.map(_.grouped(12).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse] = future.map(_.grouped(13).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse] = future.map(_.grouped(14).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse] = future.map(_.grouped(15).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse] = future.map(_.grouped(16).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse, Q: Parse] = future.map(_.grouped(17).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)), q map (Parse[Q](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse, Q: Parse, R: Parse] = future.map(_.grouped(18).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)), q map (Parse[Q](_)), r map (Parse[R](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse, Q: Parse, R: Parse, S: Parse] = future.map(_.grouped(19).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)), q map (Parse[Q](_)), r map (Parse[R](_)), s map (Parse[S](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse, Q: Parse, R: Parse, S: Parse, T: Parse] = future.map(_.grouped(20).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)), q map (Parse[Q](_)), r map (Parse[R](_)), s map (Parse[S](_)), t map (Parse[T](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse, Q: Parse, R: Parse, S: Parse, T: Parse, U: Parse] = future.map(_.grouped(21).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)), q map (Parse[Q](_)), r map (Parse[R](_)), s map (Parse[S](_)), t map (Parse[T](_)), u map (Parse[U](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse, Q: Parse, R: Parse, S: Parse, T: Parse, U: Parse, V: Parse] = future.map(_.grouped(22).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)), q map (Parse[Q](_)), r map (Parse[R](_)), s map (Parse[S](_)), t map (Parse[T](_)), u map (Parse[U](_)), v map (Parse[V](_)))
    } toList)
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
    def parse[A: Parse, B: Parse] = value.grouped(2).collect {
      case List(a, b) => (a map (Parse[A](_)), b map (Parse[B](_)))
    } toList
    def parse[A: Parse, B: Parse, C: Parse] = value.grouped(3).collect {
      case List(a, b, c) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)))
    } toList
    def parse[A: Parse, B: Parse, C: Parse, D: Parse] = value.grouped(4).collect {
      case List(a, b, c, d) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)))
    } toList
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse] = value.grouped(5).collect {
      case List(a, b, c, d, e) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)))
    } toList
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse] = value.grouped(6).collect {
      case List(a, b, c, d, e, f) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)))
    } toList
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse] = value.grouped(7).collect {
      case List(a, b, c, d, e, f, g) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)))
    } toList
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse] = value.grouped(8).collect {
      case List(a, b, c, d, e, f, g, h) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)))
    } toList
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse] = value.grouped(9).collect {
      case List(a, b, c, d, e, f, g, h, i) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)))
    } toList
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse] = value.grouped(10).collect {
      case List(a, b, c, d, e, f, g, h, i, j) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)))
    } toList
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse] = value.grouped(11).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)))
    } toList
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse] = value.grouped(12).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)))
    } toList
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse] = value.grouped(13).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)))
    } toList
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse] = value.grouped(14).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)))
    } toList
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse] = value.grouped(15).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)))
    } toList
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse] = value.grouped(16).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)))
    } toList
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse, Q: Parse] = value.grouped(17).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)), q map (Parse[Q](_)))
    } toList
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse, Q: Parse, R: Parse] = value.grouped(18).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)), q map (Parse[Q](_)), r map (Parse[R](_)))
    } toList
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse, Q: Parse, R: Parse, S: Parse] = value.grouped(19).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)), q map (Parse[Q](_)), r map (Parse[R](_)), s map (Parse[S](_)))
    } toList
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse, Q: Parse, R: Parse, S: Parse, T: Parse] = value.grouped(20).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)), q map (Parse[Q](_)), r map (Parse[R](_)), s map (Parse[S](_)), t map (Parse[T](_)))
    } toList
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse, Q: Parse, R: Parse, S: Parse, T: Parse, U: Parse] = value.grouped(21).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)), q map (Parse[Q](_)), r map (Parse[R](_)), s map (Parse[S](_)), t map (Parse[T](_)), u map (Parse[U](_)))
    } toList
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse, Q: Parse, R: Parse, S: Parse, T: Parse, U: Parse, V: Parse] = value.grouped(22).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)), q map (Parse[Q](_)), r map (Parse[R](_)), s map (Parse[S](_)), t map (Parse[T](_)), u map (Parse[U](_)), v map (Parse[V](_)))
    } toList
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

  private[redis] class ParseBulkQueued(queued: Queued[Future[Option[ByteString]]]) {
    def parse[A: Parse]: Queued[Future[Option[A]]] = queued.map(_.map(_.map(Parse(_))))
  }
  private[redis] class ParseMultiBulkQueued(queued: Queued[Future[Option[List[Option[ByteString]]]]]) {
    def parse[A: Parse]: Queued[Future[Option[List[Option[A]]]]] = queued.map(_.map(_.map(_.map(_.map(Parse(_))))))
  }
  private[redis] class ParseMultiBulkListQueued(queued: Queued[Future[List[Option[ByteString]]]]) {
    def parse[A: Parse]: Queued[Future[List[Option[A]]]] = queued.map(_.map(_.map(_.map(Parse(_)))))
    def parse[A: Parse, B: Parse] = queued.map(_.map(_.grouped(2).collect {
      case List(a, b) => (a map (Parse[A](_)), b map (Parse[B](_)))
    } toList))
    def parse[A: Parse, B: Parse, C: Parse] = queued.map(_.map(_.grouped(3).collect {
      case List(a, b, c) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)))
    } toList))
    def parse[A: Parse, B: Parse, C: Parse, D: Parse] = queued.map(_.map(_.grouped(4).collect {
      case List(a, b, c, d) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)))
    } toList))
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse] = queued.map(_.map(_.grouped(5).collect {
      case List(a, b, c, d, e) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)))
    } toList))
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse] = queued.map(_.map(_.grouped(6).collect {
      case List(a, b, c, d, e, f) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)))
    } toList))
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse] = queued.map(_.map(_.grouped(7).collect {
      case List(a, b, c, d, e, f, g) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)))
    } toList))
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse] = queued.map(_.map(_.grouped(8).collect {
      case List(a, b, c, d, e, f, g, h) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)))
    } toList))
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse] = queued.map(_.map(_.grouped(9).collect {
      case List(a, b, c, d, e, f, g, h, i) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)))
    } toList))
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse] = queued.map(_.map(_.grouped(10).collect {
      case List(a, b, c, d, e, f, g, h, i, j) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)))
    } toList))
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse] = queued.map(_.map(_.grouped(11).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)))
    } toList))
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse] = queued.map(_.map(_.grouped(12).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)))
    } toList))
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse] = queued.map(_.map(_.grouped(13).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)))
    } toList))
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse] = queued.map(_.map(_.grouped(14).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)))
    } toList))
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse] = queued.map(_.map(_.grouped(15).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)))
    } toList))
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse] = queued.map(_.map(_.grouped(16).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)))
    } toList))
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse, Q: Parse] = queued.map(_.map(_.grouped(17).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)), q map (Parse[Q](_)))
    } toList))
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse, Q: Parse, R: Parse] = queued.map(_.map(_.grouped(18).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)), q map (Parse[Q](_)), r map (Parse[R](_)))
    } toList))
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse, Q: Parse, R: Parse, S: Parse] = queued.map(_.map(_.grouped(19).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)), q map (Parse[Q](_)), r map (Parse[R](_)), s map (Parse[S](_)))
    } toList))
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse, Q: Parse, R: Parse, S: Parse, T: Parse] = queued.map(_.map(_.grouped(20).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)), q map (Parse[Q](_)), r map (Parse[R](_)), s map (Parse[S](_)), t map (Parse[T](_)))
    } toList))
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse, Q: Parse, R: Parse, S: Parse, T: Parse, U: Parse] = queued.map(_.map(_.grouped(21).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)), q map (Parse[Q](_)), r map (Parse[R](_)), s map (Parse[S](_)), t map (Parse[T](_)), u map (Parse[U](_)))
    } toList))
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse, Q: Parse, R: Parse, S: Parse, T: Parse, U: Parse, V: Parse] = queued.map(_.map(_.grouped(22).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)), q map (Parse[Q](_)), r map (Parse[R](_)), s map (Parse[S](_)), t map (Parse[T](_)), u map (Parse[U](_)), v map (Parse[V](_)))
    } toList))
  }
  private[redis] class ParseMultiBulkFlatQueued(queued: Queued[Future[Option[List[ByteString]]]]) {
    def parse[A: Parse]: Queued[Future[Option[List[A]]]] = queued.map(_.map(_.map(_.map(Parse(_)))))
  }
  private[redis] class ParseMultiBulkFlatListQueued(queued: Queued[Future[List[ByteString]]]) {
    def parse[A: Parse]: Queued[Future[List[A]]] = queued.map(_.map(_.map(Parse(_))))
  }
  private[redis] class ParseMultiBulkSetQueued(queued: Queued[Future[Set[ByteString]]]) {
    def parse[A: Parse]: Queued[Future[Set[A]]] = queued.map(_.map(_.map(Parse(_))))
  }
  private[redis] class ParseMultiBulkMapQueued(queued: Queued[Future[Map[ByteString, ByteString]]]) {
    def parse[K: Parse, V: Parse]: Queued[Future[Map[K, V]]] = queued.map(_.map(_.map(kv => (Parse[K](kv._1), Parse[V](kv._2)))))
  }
  private[redis] class ParseMultiBulkScoredQueued(queued: Queued[Future[List[(ByteString, Double)]]]) {
    def parse[A: Parse]: Queued[Future[List[(A, Double)]]] = queued.map(_.map(_.map(kv => (Parse(kv._1), kv._2))))
  }
}
