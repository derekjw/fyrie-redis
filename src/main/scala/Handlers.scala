package net.fyrie.redis
package handlers

import serialization.{ Parse }
import Parse.Implicits._

import se.scalablesolutions.akka.dispatch.{Future, CompletableFuture, DefaultCompletableFuture}

abstract class Handler[A] {
  def verify(in: String, expect: String): Unit =
    if (in != expect) throw new RedisProtocolException("Expected '" + expect + "' reply, got: " + in)

//  def parse(in: In): Response[A]

/*  def mkFuturePair: (CompletableFuture[Any], Future[B]) = {
    val f = new DefaultCompletableFuture[A](5000)
    (f.asInstanceOf[CompletableFuture[Any]], f.map(parseResult))
  }*/

  def handlers: Seq[Handler[_]]
}

abstract class SingleHandler[A] extends Handler[A] {
  val parseResult: PartialFunction[RedisType, Response[A]]

  def parse(in: RedisType): Response[A] = parseResult.orElse(parseError).apply(in)

  val parseError: PartialFunction[RedisType, Response[A]] = {
    case RedisError(err) => Error(err)
    case x: RedisType => Error(new RedisProtocolException("Unexpected: "+x))
  }

  def handlers: Seq[Handler[_]] = Nil
}

abstract class MultiHandler[A] extends Handler[Option[Stream[A]]] {

  def parse[T[_]](in: Option[Stream[T[Any]]]): Option[Stream[Any]]

}

final case class MultiExec(handlers: Seq[Handler[_]]) extends MultiHandler[Response[Any]] {
  def parse[T[_]](in: Option[Stream[T[Any]]]): Option[Stream[Any]] =
    in.map(_.map{
      case f: Future[_] =>
        f.await.result.getOrElse(throw f.exception.get)
      case r: Response[_] =>
        r.get
      case x => x
    })
}

case object NoHandler extends SingleHandler[Unit] {
  val parseResult: PartialFunction[RedisType, Response[Unit]] = { case _ => Result(()) }
}

case object Status extends SingleHandler[String] {
  val parseResult: PartialFunction[RedisType, Response[String]] = {
    case RedisString(value) => Result(value)
  }
}

case object OkStatus extends SingleHandler[Unit] {
  val parseResult: PartialFunction[RedisType, Response[Unit]] = {
    case RedisString(value) => Response(verify(value, "OK"))
  }
}

case object QueuedStatus extends SingleHandler[Unit] {
  val parseResult: PartialFunction[RedisType, Response[Unit]] = {
    case RedisString(value) => Response(verify(value, "QUEUED"))
  }
}

case object LongInt extends SingleHandler[Long] {
  val parseResult: PartialFunction[RedisType, Response[Long]] = {
    case RedisInteger(value) => Result(value)
  }
}

case object ShortInt extends SingleHandler[Int] {
  val parseResult: PartialFunction[RedisType, Response[Int]] = {
    case RedisInteger(value) => Result(value.toInt)
  }
}

case object IntAsBoolean extends SingleHandler[Boolean]  {
  val parseResult: PartialFunction[RedisType, Response[Boolean]] = {
    case RedisInteger(value) => Result(value > 0L)
  }
}

final case class Bulk[A](implicit parse: Parse[A]) extends SingleHandler[Option[A]]  {
  val parseResult: PartialFunction[RedisType, Response[Option[A]]] = {
    case RedisBulk(value) => Response(value.map(parse))
  }
}

final case class MultiBulk[A](implicit parse: Parse[A]) extends MultiHandler[Option[A]] {
  def parse[T[_]](in: Option[Stream[T[Any]]]): Option[Stream[Any]] =
    in.map(_.map{
      case f: Future[_] =>
        f.await.result.getOrElse(throw f.exception.get)
      case r: Response[_] =>
        r.get
      case x => x
    })

  def handlers = Stream.continually(Bulk[A]())
}

final case class MultiBulkAsPairs[K, V](implicit parseK: Parse[K], parseV: Parse[V]) extends MultiHandler[(K, V)] {
  def parse[T[_]](in: Option[Stream[T[Any]]]): Option[Stream[Any]] =
    in.map(_.map{
      case f: Future[_] =>
        f.await.result.getOrElse(throw f.exception.get)
      case r: Response[_] =>
        r.get
      case x => x
    }.grouped(2).toStream.flatMap{
      case Stream(Some(k), Some(v)) => Some((k, v))
      case _ => None
    })

  def handlers = Stream.continually(Stream(Bulk[K](), Bulk[V]())).flatten
}

final case class MultiBulkWithScores[A](implicit parse: Parse[A]) extends MultiHandler[(A, Double)] {
  def parse[T[_]](in: Option[Stream[T[Any]]]): Option[Stream[Any]] =
    in.map(_.map{
      case f: Future[_] =>
        f.await.result.getOrElse(throw f.exception.get)
      case r: Response[_] =>
        r.get
      case x => x
    }.grouped(2).toStream.flatMap{
      case Stream(Some(k), Some(v)) => Some((k, v))
      case _ => None
    })

  def handlers = Stream.continually(Stream(Bulk[A](), Bulk[Double]())).flatten
}

final case class MultiBulkAsFlat[A](implicit parse: Parse[A]) extends MultiHandler[A] {
  def parse[T[_]](in: Option[Stream[T[Any]]]): Option[Stream[Any]] =
    in.map(_.map{
      case f: Future[_] =>
        f.await.result.getOrElse(throw f.exception.get)
      case r: Response[_] =>
        r.get
      case x => x
    }.flatMap{
      case x: Some[_] => x
      case _ => None
    })

  def handlers = Stream.continually(Bulk[A]())
}
