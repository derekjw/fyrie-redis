package net.fyrie

import akka.util.{ ByteString, Duration }
import akka.dispatch.{ Future, Promise }
import redis.serialization.Parse

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

package object redis {
  implicit def doubleToRedisScore(value: Double): RedisScore = InclusiveScore(value)

  implicit def parseBulk(result: Option[ByteString]) = new ParseBulk[({ type λ[α] = α })#λ](result)
  implicit def parseBulk(result: Future[Option[ByteString]]) = new ParseBulk[Future](result)
  implicit def parseBulk(result: Queued[Future[Option[ByteString]]]) = new ParseBulk[({ type λ[α] = Queued[Future[α]] })#λ](result)

  implicit def parseMultiBulk(result: Option[List[Option[ByteString]]]) = new ParseMultiBulk[({ type λ[α] = α })#λ](result)
  implicit def parseMultiBulk(result: Future[Option[List[Option[ByteString]]]]) = new ParseMultiBulk[Future](result)
  implicit def parseMultiBulk(result: Queued[Future[Option[List[Option[ByteString]]]]]) = new ParseMultiBulk[({ type λ[α] = Queued[Future[α]] })#λ](result)

  implicit def parseMultiBulkList(result: List[Option[ByteString]]) = new ParseMultiBulkList[({ type λ[α] = α })#λ](result)
  implicit def parseMultiBulkList(result: Future[List[Option[ByteString]]]) = new ParseMultiBulkList[Future](result)
  implicit def parseMultiBulkList(result: Queued[Future[List[Option[ByteString]]]]) = new ParseMultiBulkList[({ type λ[α] = Queued[Future[α]] })#λ](result)

  implicit def parseMultiBulkFlat(result: Option[List[ByteString]]) = new ParseMultiBulkFlat[({ type λ[α] = α })#λ](result)
  implicit def parseMultiBulkFlat(result: Future[Option[List[ByteString]]]) = new ParseMultiBulkFlat[Future](result)
  implicit def parseMultiBulkFlat(result: Queued[Future[Option[List[ByteString]]]]) = new ParseMultiBulkFlat[({ type λ[α] = Queued[Future[α]] })#λ](result)

  implicit def parseMultiBulkFlatList(result: List[ByteString]) = new ParseMultiBulkFlatList[({ type λ[α] = α })#λ](result)
  implicit def parseMultiBulkFlatList(result: Future[List[ByteString]]) = new ParseMultiBulkFlatList[Future](result)
  implicit def parseMultiBulkFlatList(result: Queued[Future[List[ByteString]]]) = new ParseMultiBulkFlatList[({ type λ[α] = Queued[Future[α]] })#λ](result)

  implicit def parseMultiBulkSet(result: Set[ByteString]) = new ParseMultiBulkSet[({ type λ[α] = α })#λ](result)
  implicit def parseMultiBulkSet(result: Future[Set[ByteString]]) = new ParseMultiBulkSet[Future](result)
  implicit def parseMultiBulkSet(result: Queued[Future[Set[ByteString]]]) = new ParseMultiBulkSet[({ type λ[α] = Queued[Future[α]] })#λ](result)

  implicit def parseMultiBulkMap(result: Map[ByteString, ByteString]) = new ParseMultiBulkMap[({ type λ[α] = α })#λ](result)
  implicit def parseMultiBulkMap(result: Future[Map[ByteString, ByteString]]) = new ParseMultiBulkMap[Future](result)
  implicit def parseMultiBulkMap(result: Queued[Future[Map[ByteString, ByteString]]]) = new ParseMultiBulkMap[({ type λ[α] = Queued[Future[α]] })#λ](result)

  implicit def parseMultiBulkScored(result: List[(ByteString, Double)]) = new ParseMultiBulkScored[({ type λ[α] = α })#λ](result)
  implicit def parseMultiBulkScored(result: Future[List[(ByteString, Double)]]) = new ParseMultiBulkScored[Future](result)
  implicit def parseMultiBulkScored(result: Queued[Future[List[(ByteString, Double)]]]) = new ParseMultiBulkScored[({ type λ[α] = Queued[Future[α]] })#λ](result)

  private[redis] class ParseBulk[Result[_]](value: Result[Option[ByteString]])(implicit f: ResultFunctor[Result]) {
    def parse[A: Parse]: Result[Option[A]] = f.fmap(value)(_.map(Parse(_)))
  }
  private[redis] class ParseMultiBulk[Result[_]](value: Result[Option[List[Option[ByteString]]]])(implicit f: ResultFunctor[Result]) {
    def parse[A: Parse]: Result[Option[List[Option[A]]]] = f.fmap(value)(_.map(_.map(_.map(Parse(_)))))
  }
  private[redis] class ParseMultiBulkList[Result[_]](value: Result[List[Option[ByteString]]])(implicit f: ResultFunctor[Result]) {
    def parse[A: Parse]: Result[List[Option[A]]] = f.fmap(value)(_.map(_.map(Parse(_))))
    def parse[A: Parse, B: Parse] = f.fmap(value)(_.grouped(2).collect {
      case List(a, b) ⇒ (a map (Parse[A](_)), b map (Parse[B](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse] = f.fmap(value)(_.grouped(3).collect {
      case List(a, b, c) ⇒ (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse] = f.fmap(value)(_.grouped(4).collect {
      case List(a, b, c, d) ⇒ (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse] = f.fmap(value)(_.grouped(5).collect {
      case List(a, b, c, d, e) ⇒ (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse] = f.fmap(value)(_.grouped(6).collect {
      case List(a, b, c, d, e, f) ⇒ (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse] = f.fmap(value)(_.grouped(7).collect {
      case List(a, b, c, d, e, f, g) ⇒ (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse] = f.fmap(value)(_.grouped(8).collect {
      case List(a, b, c, d, e, f, g, h) ⇒ (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse] = f.fmap(value)(_.grouped(9).collect {
      case List(a, b, c, d, e, f, g, h, i) ⇒ (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse] = f.fmap(value)(_.grouped(10).collect {
      case List(a, b, c, d, e, f, g, h, i, j) ⇒ (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse] = f.fmap(value)(_.grouped(11).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k) ⇒ (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse] = f.fmap(value)(_.grouped(12).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l) ⇒ (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse] = f.fmap(value)(_.grouped(13).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m) ⇒ (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse] = f.fmap(value)(_.grouped(14).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n) ⇒ (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse] = f.fmap(value)(_.grouped(15).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o) ⇒ (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse] = f.fmap(value)(_.grouped(16).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p) ⇒ (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse, Q: Parse] = f.fmap(value)(_.grouped(17).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q) ⇒ (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)), q map (Parse[Q](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse, Q: Parse, R: Parse] = f.fmap(value)(_.grouped(18).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r) ⇒ (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)), q map (Parse[Q](_)), r map (Parse[R](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse, Q: Parse, R: Parse, S: Parse] = f.fmap(value)(_.grouped(19).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s) ⇒ (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)), q map (Parse[Q](_)), r map (Parse[R](_)), s map (Parse[S](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse, Q: Parse, R: Parse, S: Parse, T: Parse] = f.fmap(value)(_.grouped(20).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t) ⇒ (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)), q map (Parse[Q](_)), r map (Parse[R](_)), s map (Parse[S](_)), t map (Parse[T](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse, Q: Parse, R: Parse, S: Parse, T: Parse, U: Parse] = f.fmap(value)(_.grouped(21).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u) ⇒ (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)), q map (Parse[Q](_)), r map (Parse[R](_)), s map (Parse[S](_)), t map (Parse[T](_)), u map (Parse[U](_)))
    } toList)
    def parse[A: Parse, B: Parse, C: Parse, D: Parse, E: Parse, F: Parse, G: Parse, H: Parse, I: Parse, J: Parse, K: Parse, L: Parse, M: Parse, N: Parse, O: Parse, P: Parse, Q: Parse, R: Parse, S: Parse, T: Parse, U: Parse, V: Parse] = f.fmap(value)(_.grouped(22).collect {
      case List(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v) ⇒ (a map (Parse[A](_)), b map (Parse[B](_)), c map (Parse[C](_)), d map (Parse[D](_)), e map (Parse[E](_)), f map (Parse[F](_)), g map (Parse[G](_)), h map (Parse[H](_)), i map (Parse[I](_)), j map (Parse[J](_)), k map (Parse[K](_)), l map (Parse[L](_)), m map (Parse[M](_)), n map (Parse[N](_)), o map (Parse[O](_)), p map (Parse[P](_)), q map (Parse[Q](_)), r map (Parse[R](_)), s map (Parse[S](_)), t map (Parse[T](_)), u map (Parse[U](_)), v map (Parse[V](_)))
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
    def parse[K: Parse, V: Parse]: Result[Map[K, V]] = f.fmap(value)(_.map(kv ⇒ (Parse[K](kv._1), Parse[V](kv._2))))
  }
  private[redis] class ParseMultiBulkScored[Result[_]](value: Result[List[(ByteString, Double)]])(implicit f: ResultFunctor[Result]) {
    def parse[A: Parse]: Result[List[(A, Double)]] = f.fmap(value)(_.map(kv ⇒ (Parse(kv._1), kv._2)))
  }
}
