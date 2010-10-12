package net.fyrie.redis
package handlers

import serialization.{ Parse }

class Handler[A](f: (RedisStreamReader, BaseHandlers) => A) {
  def apply(r: RedisStreamReader, h: BaseHandlers): A = f(r, h)
}

case class MultiExec(handlers: Seq[Handler[_]]) extends Handler[Result[Stream[_]]]({ (r, h) =>
  OkStatus(r, h)
  // Need some kind of protection against protocol exceptions, like the following, but need a way of testing:
  // handlers.filter{x => try{QueuedStatus(r); true} catch {case e: RedisProtocolException => false}}
  handlers.foreach(x => QueuedStatus(r, h))
  h.multiexec(r, handlers)
})

case object NoHandler extends Handler[Unit]( (r, h) => error("Can't handle reply"))

case object Status extends Handler( (r, h) => h.status(r))

case object OkStatus extends Handler( (r, h) => h.verify(h.status(r), "OK"))

case object QueuedStatus extends Handler( (r, h) => h.verify(h.status(r), "QUEUED"))

case object ShortInt extends Handler( (r, h) => h.integer(r).toInt)

case object IntAsBoolean extends Handler( (r, h) => h.integer(r) >= 1L)

case object LongInt extends Handler( (r, h) => h.integer(r))

case class Bulk[A](implicit parse: Parse[A]) extends Handler[Result[A]](
  (r, h) => h.bulk(r).map(x => parse(x)))

case class MultiBulk[A](implicit parse: Parse[A]) extends Handler[Result[Stream[Option[A]]]](
  (r, h) => h.multibulk(r).map(_.map(_.map(x => parse(x)))))

case class MultiBulkAsPairs[K, V](implicit parseK: Parse[K], parseV: Parse[V]) extends Handler[Result[Stream[(K, V)]]](
  (r, h) => h.multibulk(r).map(_.grouped(2).collect { case Seq(Some(k), Some(v)) => (parseK(k), parseV(v)) }.toStream))

case class MultiBulkWithScores[A](implicit parse: Parse[A]) extends Handler[Result[Stream[(A, Double)]]](
  (r, h) => h.multibulk(r).map(_.grouped(2).collect { case Seq(Some(k), Some(v)) => (parse(k), new String(v).toDouble) }.toStream))

case class MultiBulkAsFlat[A](implicit parse: Parse[A]) extends Handler[Result[Stream[A]]](
  (r, h) => h.multibulk(r).map(_.flatMap(_.map(x => parse(x)))))

class BaseHandlers {

  private val lazyResults = new collection.mutable.Queue[Function0[Unit]]

  def forceLazyResults { while (!lazyResults.isEmpty) { try { lazyResults.dequeue().apply } catch { case e => e } } }

  def verify(in: String, expect: String): Unit =
    if (in != expect) throw new RedisProtocolException("Expected '" + expect + "' reply, got: " + in)

  def parse(marker: Char, r: RedisStreamReader): String = {
    val reply = r.readReply
    val m = reply(0).toChar
    val result = new String(reply, 1, reply.size - 1)
    if (m == marker) {
      result
    } else {
      if (m == '-') {
        throw new RedisErrorException(result)
      } else {
        throw new RedisProtocolException("Expected a '" + marker + "' reply, got " + m + result)
      }
    }
  }

  def status(r: RedisStreamReader): String = parse('+', r)

  def integer(r: RedisStreamReader): Long = parse(':', r).toLong

  def bulk(r: RedisStreamReader): Result[Array[Byte]] = {
    val res = Result{ parse('$', r).toInt match {
      case -1 => throw new NoSuchElementException
      case l => r.readBulk(l)
    }}
    lazyResults enqueue (() => res.force)
    res
  }

  def multibulk(r: RedisStreamReader): Result[Stream[Option[Array[Byte]]]] = {
    val res = Result{ parse('*', r).toInt match {
      case -1 => throw new java.util.NoSuchElementException
      case n => Stream.fill[Option[Array[Byte]]](n)(bulk(r).toOption)
    }}
    lazyResults enqueue (() => res.map(_.force).force)
    res
  }

  def multiexec(r: RedisStreamReader, handlers: Seq[Handler[_]]): Result[Stream[_]] = {
    val res = Result{ parse('*', r).toInt match {
      case -1 => throw new java.util.NoSuchElementException
      case n => handlers.toStream.map{h => h(r, this)}
    }}
    lazyResults enqueue (() => res.map(_.force).force)
    res
  }
}
