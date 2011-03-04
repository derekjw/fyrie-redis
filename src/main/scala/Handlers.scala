package net.fyrie.redis
package handlers

import serialization.{ Parse }
import Parse.Implicits._
import utils._

import akka.dispatch.{Future, CompletableFuture, DefaultCompletableFuture}

abstract class Handler[A](implicit val manifest: Manifest[A]) {
  def verify(in: String, expect: String): Unit =
    if (in != expect) throw new RedisProtocolException("Expected '" + expect + "' reply, got: " + in)

  def handlers: Seq[Handler[_]]
}

abstract class SingleHandler[A: RedisType: Manifest, B: Manifest] extends Handler[B] {
  implicit val inputManifest = implicitly[Manifest[A]]

  def parse(in: A): Response[B]

  def handlers: Seq[Handler[_]] = Nil
}

abstract class MultiHandler[A: Manifest] extends Handler[Option[Stream[A]]] {
  def parse(in: Option[Stream[Response[Any]]]): Option[Stream[A]]

  def parser(length: Option[Int]) = Result(MultiParser(length, parse _)(this.manifest))
}

case class MultiParser[A](length: Option[Int], parse: Option[Stream[Response[Any]]] => A)(implicit val manifest: Manifest[A])

final case class MultiExec(handlers: Seq[Handler[_]]) extends MultiHandler[Any] {
  def parse(in: Option[Stream[Response[Any]]]): Option[Stream[Any]] = in.map(_.map(_.get))
}

case object NoHandler extends SingleHandler[Exception, Unit] {
  def parse(in: Exception): Response[Unit] = throw in
}

case object Status extends SingleHandler[String, String] {
  def parse(in: String): Response[String] = Result(in)
}

case object OkStatus extends SingleHandler[String, Unit] {
  def parse(in: String): Response[Unit] = Response(verify(in, "OK"))
}

case object QueuedStatus extends SingleHandler[String, Unit] {
  def parse(in: String): Response[Unit] = Response(verify(in, "QUEUED"))
}

case object LongInt extends SingleHandler[Long, Long] {
  def parse(in: Long): Response[Long] = Result(in)
}

case object ShortInt extends SingleHandler[Long, Int] {
  def parse(in: Long): Response[Int] = Response(in.toInt)
}

case object IntAsBoolean extends SingleHandler[Long, Boolean] {
  def parse(in: Long): Response[Boolean] = Result(in > 0L)
}

final case class Bulk[A: Parse: Manifest]() extends SingleHandler[Option[Array[Byte]], Option[A]] {
  def parse(in: Option[Array[Byte]]): Response[Option[A]] = Response(in.map(x => x))

  def lazyParse(in: Option[Array[Byte]]) = LazyResponse(parse(in))
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
