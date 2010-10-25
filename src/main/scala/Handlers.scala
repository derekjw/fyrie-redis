package net.fyrie.redis
package handlers

import serialization.{ Parse }
import Parse.Implicits._

import se.scalablesolutions.akka.dispatch.{Future, CompletableFuture, DefaultCompletableFuture}

abstract class Handler[A,B] {
  def verify(in: String, expect: String): Unit =
    if (in != expect) throw new RedisProtocolException("Expected '" + expect + "' reply, got: " + in)

  def mkFuturePair: (CompletableFuture[Any], Future[B]) = {
    val f = new DefaultCompletableFuture[A](5000)
    (f.asInstanceOf[CompletableFuture[Any]], f.map(parseResult))
  }

  def parseResult(in: A): B

  def handlers: Seq[Handler[_,_]]
}

trait SingleHandler {
  def handlers: Seq[Handler[_,_]] = Nil
}

final case class MultiExec(handlers: Seq[Handler[_,_]]) extends Handler[Option[Stream[Future[_]]], Option[Stream[_]]] {
  // This should throw an exception if a future contains one
  def parseResult(in: Option[Stream[Future[_]]]): Option[Stream[_]] =
    in.map(_.flatMap(_.await.result))
}

case object NoHandler extends Handler[Unit,Unit] with SingleHandler {
  def parseResult(in: Unit): Unit = ()
}

case object Status extends Handler[RedisString,String] with SingleHandler {
  def parseResult(in: RedisString): String = in.value
}

case object OkStatus extends Handler[RedisString,Unit] with SingleHandler {
  def parseResult(in: RedisString): Unit = verify(in.value, "OK")
}

case object QueuedStatus extends Handler[RedisString,Unit] with SingleHandler {
  def parseResult(in: RedisString): Unit = verify(in.value, "QUEUED")
}

case object LongInt extends Handler[RedisInteger,Long] with SingleHandler {
  def parseResult(in: RedisInteger): Long = in.value
}

case object ShortInt extends Handler[RedisInteger,Int] with SingleHandler {
  def parseResult(in: RedisInteger): Int = in.value.toInt
}

case object IntAsBoolean extends Handler[RedisInteger,Boolean] with SingleHandler  {
  def parseResult(in: RedisInteger): Boolean = in.value > 0L
}

final case class Bulk[A](implicit parse: Parse[A]) extends Handler[RedisBulk,Option[A]] with SingleHandler  {
  def parseResult(in: RedisBulk): Option[A] = in.value.map(parse)
}

final case class MultiBulk[A](implicit parse: Parse[A]) extends Handler[Option[Stream[Future[RedisBulk]]], Option[Stream[Option[A]]]] {
  def parseResult(in: Option[Stream[Future[RedisBulk]]]): Option[Stream[Option[A]]] =
    in.map(_.map(_.await.result.flatMap(_.value.map(parse))))

  def handlers = Stream.continually(Bulk[A]())
}

final case class MultiBulkAsPairs[K, V](implicit parseK: Parse[K], parseV: Parse[V]) extends Handler[Option[Stream[Future[RedisBulk]]], Option[Stream[(K, V)]]] {
  def parseResult(in: Option[Stream[Future[RedisBulk]]]): Option[Stream[(K,V)]] =
    in.map(_.map(_.await.result.get).grouped(2).toStream.flatMap{
      case Stream(RedisBulk(Some(k)), RedisBulk(Some(v))) => Some(parseK(k), parseV(v))
      case _ => None
    })

  def handlers = Stream.continually(Stream(Bulk[K](), Bulk[V]())).flatten
}

final case class MultiBulkWithScores[A](implicit parse: Parse[A]) extends Handler[Option[Stream[Future[RedisBulk]]], Option[Stream[(A, Double)]]] {
  def parseResult(in: Option[Stream[Future[RedisBulk]]]): Option[Stream[(A, Double)]] =
    in.map(_.map(_.await.result.get).grouped(2).toStream.flatMap{
      case Stream(RedisBulk(Some(k)), RedisBulk(Some(v))) => Some(parse(k), parseDouble(v))
      case _ => None
    })

  def handlers = Stream.continually(Stream(Bulk[A](), Bulk[Double]())).flatten
}

final case class MultiBulkAsFlat[A](implicit parse: Parse[A]) extends Handler[Option[Stream[Future[RedisBulk]]],Option[Stream[A]]] {
  def parseResult(in: Option[Stream[Future[RedisBulk]]]): Option[Stream[A]] =
    in.map(_.map(_.await.result.get).flatMap(_.value.map(parse)))

  def handlers = Stream.continually(Bulk[A]())
}
