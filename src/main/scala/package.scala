package net.fyrie

import akka.util.ByteString
import akka.dispatch.{ Future, Promise }
import redis.serialization.Parse

package object redis extends ImplicitParseLow {
  implicit def doubleToRedisScore(value: Double): RedisScore = InclusiveScore(value)

  implicit def parseMultiBulk[Result[_]: ResultFunctor](result: Result[Option[List[Option[ByteString]]]]): ParseMultiBulk[Result] = new ParseMultiBulk[Result](result)
  implicit def parseMultiBulkList[Result[_]: ResultFunctor](result: Result[List[Option[ByteString]]]): ParseMultiBulkList[Result] = new ParseMultiBulkList[Result](result)
  implicit def parseMultiBulkFlat[Result[_]: ResultFunctor](result: Result[Option[List[ByteString]]]): ParseMultiBulkFlat[Result] = new ParseMultiBulkFlat[Result](result)
  implicit def parseMultiBulkSet[Result[_]: ResultFunctor](result: Result[Set[ByteString]]): ParseMultiBulkSet[Result] = new ParseMultiBulkSet[Result](result)
  implicit def parseMultiBulkMap[Result[_]: ResultFunctor](result: Result[Map[ByteString, ByteString]]): ParseMultiBulkMap[Result] = new ParseMultiBulkMap[Result](result)
  implicit def parseMultiBulkScored[Result[_]: ResultFunctor](result: Result[List[(ByteString, Double)]]): ParseMultiBulkScored[Result] = new ParseMultiBulkScored[Result](result)

}

package redis {

  trait ImplicitParseLow {
    implicit def parseBulk[Result[_]: ResultFunctor](result: Result[Option[ByteString]]): ParseBulk[Result] = new ParseBulk[Result](result)
    implicit def parseMultiBulkFlatList[Result[_]: ResultFunctor](result: Result[List[ByteString]]): ParseMultiBulkFlatList[Result] = new ParseMultiBulkFlatList[Result](result)
  }

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

  private[redis] class ParseBulk[Result[_]](value: Result[Option[ByteString]])(implicit f: ResultFunctor[Result]) {
    def parse[A: Parse]: Result[Option[A]] = f.fmap(value)(_.map(Parse(_)))
  }
  private[redis] class ParseMultiBulk[Result[_]](value: Result[Option[List[Option[ByteString]]]])(implicit f: ResultFunctor[Result]) {
    def parse[A: Parse]: Result[Option[List[Option[A]]]] = f.fmap(value)(_.map(_.map(_.map(Parse(_)))))
  }
  private[redis] class ParseMultiBulkList[Result[_]](value: Result[List[Option[ByteString]]])(implicit f: ResultFunctor[Result]) {
    def parse[A: Parse]: Result[List[Option[A]]] = f.fmap(value)(_.map(_.map(Parse(_))))
    def parse[A: Parse, B: Parse] = f.fmap(value)(_.grouped(2).collect {
      case List(a, b) => (a map (Parse[A](_)), b map (Parse[B](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse] = f.fmap(value)(_.grouped(3).collect {
      case List(a, b, c) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse] = f.fmap(value)(_.grouped(4).collect {
      case List(a, b, c, d) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse] = f.fmap(value)(_.grouped(5).collect {
      case List(a, b, c, d, e) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse] = f.fmap(value)(_.grouped(6).collect {
      case List(a, b, c, d, e, f) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse] = f.fmap(value)(_.grouped(7).collect {
      case List(a, b, c, d, e, f, g) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse] = f.fmap(value)(_.grouped(8).collect {
      case List(a, b, c, d, e, f, g, h) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse] = f.fmap(value)(_.grouped(9).collect {
      case List(a, b, c, d, e, f, g, h, i) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse] = f.fmap(value)(_.grouped(10).collect {
      case List(a, b, c, d, e, f, g, h, i, j) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse] = f.fmap(value)(_.grouped(11).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse] = f.fmap(value)(_.grouped(12).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse] = f.fmap(value)(_.grouped(13).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse] = f.fmap(value)(_.grouped(14).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse] = f.fmap(value)(_.grouped(15).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse] = f.fmap(value)(_.grouped(16).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse, Q: Parse] = f.fmap(value)(_.grouped(17).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)), q map (Parse[Q](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse, Q: Parse, R: Parse] = f.fmap(value)(_.grouped(18).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)), q map (Parse[Q](_)), r map (Parse[R](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse, Q: Parse, R: Parse, S: Parse] = f.fmap(value)(_.grouped(19).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)), q map (Parse[Q](_)), r map (Parse[R](_)), s map (Parse[S](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse, Q: Parse, R: Parse, S: Parse, T: Parse] = f.fmap(value)(_.grouped(20).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)), q map (Parse[Q](_)), r map (Parse[R](_)), s map (Parse[S](_)), t map (Parse[T](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse, Q: Parse, R: Parse, S: Parse, T: Parse, U: Parse] = f.fmap(value)(_.grouped(21).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)), q map (Parse[Q](_)), r map (Parse[R](_)), s map (Parse[S](_)), t map (Parse[T](_)), u map (Parse[U](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse, Q: Parse, R: Parse, S: Parse, T: Parse, U: Parse, V: Parse] = f.fmap(value)(_.grouped(22).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v) => (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)), q map (Parse[Q](_)), r map (Parse[R](_)), s map (Parse[S](_)), t map (Parse[T](_)), u map (Parse[U](_)), v map (Parse[V](_)))
    } toList)
  }
  private[redis] class ParseMultiBulkFlat[Result[_]](value: Result[Option[List[ByteString]]])(implicit f: ResultFunctor[Result]) {
    def parse[A: Parse]: Result[Option[List[A]]] = f.fmap(value)(_.map(_.map(Parse(_))))
  }
  private[redis] class ParseMultiBulkFlatList[Result[_]](value: Result[List[ByteString]])(implicit f: ResultFunctor[Result]) {
    def parse[A: Parse]: Result[List[A]] = f.fmap(value)(_.map(Parse(_)))
  }
  private[redis] class ParseMultiBulkSet[Result[_]](value: Result[Set[ByteString]])(implicit f: ResultFunctor[Result]) {
    def parse[A: Parse]: Result[Set[A]] = f.fmap(value)(_.map(Parse(_)))
  }
  private[redis] class ParseMultiBulkMap[Result[_]](value: Result[Map[ByteString, ByteString]])(implicit f: ResultFunctor[Result]) {
    def parse[K: Parse, V: Parse]: Result[Map[K, V]] = f.fmap(value)(_.map(kv => (Parse[K](kv._1), Parse[V](kv._2))))
  }
  private[redis] class ParseMultiBulkScored[Result[_]](value: Result[List[(ByteString, Double)]])(implicit f: ResultFunctor[Result]) {
    def parse[A: Parse]: Result[List[(A, Double)]] = f.fmap(value)(_.map(kv => (Parse(kv._1), kv._2)))
  }
}
