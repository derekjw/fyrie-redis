package net.fyrie.redis
package handlers

import serialization.{ Parse }
import Parse.Implicits._
import utils._

import se.scalablesolutions.akka.dispatch.{Future, CompletableFuture, DefaultCompletableFuture}

abstract class Handler[A](implicit val manifest: Manifest[A]) {
  def verify(in: String, expect: String): Unit =
    if (in != expect) throw new RedisProtocolException("Expected '" + expect + "' reply, got: " + in)

  def handlers: Seq[Handler[_]]
}

abstract class SingleHandler[A: Manifest, B: Manifest] extends Handler[B] {
  implicit val inputManifest = implicitly[Manifest[A]]

  def parse(in: A): Response[B]

  def handlers: Seq[Handler[_]] = Nil
}

abstract class MultiHandler[A: Manifest] extends Handler[Option[Stream[A]]] {
  def parse(in: Option[Stream[Response[Any]]]): Option[Stream[A]]
}

final case class MultiExec(handlers: Seq[Handler[_]]) extends MultiHandler[Any] {
  def parse(in: Option[Stream[Response[Any]]]): Option[Stream[Any]] = in.map(_.map(_.get))
}

case object NoHandler extends SingleHandler[Unit, Unit] {
  def parse(in: Unit): Response[Unit] = throw new RedisErrorException("No handler")
}

case object Status extends SingleHandler[RedisString, String] {
  def parse(in: RedisString): Response[String] = Result(in.value)
}

case object OkStatus extends SingleHandler[RedisString, Unit] {
  def parse(in: RedisString): Response[Unit] = Response(verify(in.value, "OK"))
}

case object QueuedStatus extends SingleHandler[RedisString, Unit] {
  def parse(in: RedisString): Response[Unit] = Response(verify(in.value, "QUEUED"))
}

case object LongInt extends SingleHandler[RedisInteger, Long] {
  def parse(in: RedisInteger): Response[Long] = Result(in.value)
}

case object ShortInt extends SingleHandler[RedisInteger, Int] {
  def parse(in: RedisInteger): Response[Int] = Response(in.value.toInt)
}

case object IntAsBoolean extends SingleHandler[RedisInteger, Boolean] {
  def parse(in: RedisInteger): Response[Boolean] = Result(in.value > 0L)
}

final case class Bulk[A: Parse: Manifest]() extends SingleHandler[RedisBulk, Option[A]] {
  def parse(in: RedisBulk): Response[Option[A]] = Response(in.value.map(x => x))
}

final case class MultiBulk[A: Parse: Manifest]() extends MultiHandler[Option[A]] {
  def parse(in: Option[Stream[Response[Any]]]): Option[Stream[Option[A]]] =
    in.map(_.map(_.asA[Option[A]].get))

  def handlers = Stream.continually(Bulk[A]())
}

final case class MultiBulkAsPairs[K: Parse: Manifest, V: Parse: Manifest]() extends MultiHandler[(K, V)] {
  def parse(in: Option[Stream[Response[Any]]]): Option[Stream[(K,V)]] =
    in.map(_.grouped(2).toStream.flatMap{
      case Stream(k, v) => (k.asA[Option[K]].get, v.asA[Option[V]].get) match {
        case (Some(k), Some(v)) => Some((k, v))
        case _ => None
      }
      case _ => None
    })

  def handlers = Stream.continually(Stream(Bulk[K](), Bulk[V]())).flatten
}

final case class MultiBulkWithScores[A: Parse: Manifest]() extends MultiHandler[(A, Double)] {
  def parse(in: Option[Stream[Response[Any]]]): Option[Stream[(A, Double)]] =
    in.map(_.grouped(2).toStream.flatMap{
      case Stream(k, v) => (k.asA[Option[A]].get, v.asA[Option[Double]].get) match {
        case (Some(k), Some(v)) => Some((k, v))
        case _ => None
      }
      case _ => None
    })

  def handlers = Stream.continually(Stream(Bulk[A](), Bulk[Double]())).flatten
}

final case class MultiBulkAsFlat[A: Parse: Manifest]() extends MultiHandler[A] {
  def parse(in: Option[Stream[Response[Any]]]): Option[Stream[A]] =
    in.map(_.flatMap(_.asA[Option[A]].get))

  def handlers = Stream.continually(Bulk[A]())
}
