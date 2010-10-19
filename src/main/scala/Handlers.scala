package net.fyrie.redis
package handlers

import serialization.{ Parse }
import Parse.Implicits._

import se.scalablesolutions.akka.dispatch.{CompletableFuture, DefaultCompletableFuture}

abstract class Handler[A] {
  def apply(data: Array[Byte], future: Option[CompletableFuture[Any]]): Seq[(Handler[_], Option[CompletableFuture[Any]])]

  def verify(in: String, expect: String): Unit =
    if (in != expect) throw new RedisProtocolException("Expected '" + expect + "' reply, got: " + in)

  def string(in: Array[Byte]): String = new String(in, "UTF-8")

  def complete(future: Option[CompletableFuture[Any]], value: => Any) =
    future foreach { f =>
      try {
        f.completeWithResult(Result(value))
      } catch {
        case e: Exception => f.completeWithException(e)
      }
    }
}


final case class MultiExec(handlers: Seq[Handler[_]]) extends Handler[Option[Stream[_]]] {
  def apply(data: Array[Byte], future: Option[CompletableFuture[Any]]) = {
    OkStatus(data, None)
    val futures = Stream.fill[Option[CompletableFuture[Any]]](handlers.length)(if (future.isDefined) Some(new DefaultCompletableFuture[Any](5000)) else None)
    complete(future, Some(futures.collect{case Some(f) => f.await.result}.collect{case Some(v) => v}))
    Stream.fill(handlers.length)(QueuedStatus).foldLeft(Stream[(Handler[_], Option[CompletableFuture[Any]])]((MultiExecResult(handlers.toStream zip futures), future))){ case (s, h) => (h, None) #:: s }
  }
}

final case class MultiExecResult(hfs: Seq[(Handler[_], Option[CompletableFuture[Any]])]) extends Handler[Option[Stream[_]]] {
  def apply(data: Array[Byte], future: Option[CompletableFuture[Any]]) = {
    hfs
  }
}

case object NoHandler extends Handler[Unit] {
  def apply(data: Array[Byte], future: Option[CompletableFuture[Any]]) = {
    future foreach (_.completeWithException( new Exception("Can't handle reply")))
    Nil
  }
}

case object Status extends Handler[String] {
  def apply(data: Array[Byte], future: Option[CompletableFuture[Any]]) = {
    complete(future, string(data))
    Nil
  }
}

case object OkStatus extends Handler[Unit] {
  def apply(data: Array[Byte], future: Option[CompletableFuture[Any]]) = {
    complete(future, verify(string(data), "OK"))
    Nil
  }
}

case object QueuedStatus extends Handler[Unit] {
  def apply(data: Array[Byte], future: Option[CompletableFuture[Any]]) = {
    complete(future, verify(string(data), "QUEUED"))
    Nil
  }
}

case object ShortInt extends Handler[Int] {
  def apply(data: Array[Byte], future: Option[CompletableFuture[Any]]) = {
    complete(future, string(data).toInt)
    Nil
  }
}

case object LongInt extends Handler[Long] {
  def apply(data: Array[Byte], future: Option[CompletableFuture[Any]]) = {
    complete(future, string(data).toLong)
    Nil
  }
}

case object IntAsBoolean extends Handler[Boolean] {
  def apply(data: Array[Byte], future: Option[CompletableFuture[Any]]) = {
    complete(future, string(data).toInt >= 1)
    Nil
  }
}

final case class Bulk[A](implicit parse: Parse[A]) extends Handler[Option[A]] {
  def apply(data: Array[Byte], future: Option[CompletableFuture[Any]]) = {
    complete(future, Some(parse(data)))
    Nil
  }
}

final case class MultiBulk[A](implicit parse: Parse[A]) extends Handler[Option[Stream[Option[A]]]] {
  def apply(data: Array[Byte], future: Option[CompletableFuture[Any]]) = {
    val futures = Stream.fill[Option[CompletableFuture[Any]]](string(data).toInt)(if (future.isDefined) Some(new DefaultCompletableFuture[Any](5000)) else None)
    complete(future, Some(futures.flatMap(_.flatMap(_.await.result)).flatMap{case Result(v) => Some(v); case _ => None}))
    futures.map(f => (Bulk[A](), f))
  }
}

final case class MultiBulkAsPairs[K, V](implicit parseK: Parse[K], parseV: Parse[V]) extends Handler[Option[Stream[(K, V)]]] {
  def apply(data: Array[Byte], future: Option[CompletableFuture[Any]]) = {
    val futures = Stream.fill[Option[CompletableFuture[Any]]](string(data).toInt)(if (future.isDefined) Some(new DefaultCompletableFuture[Any](5000)) else None)
    complete(future, Some(futures.collect{case Some(f) => f.await.result}.grouped(2).collect{case Seq(Some(Result(Some(k))), Some(Result(Some(v)))) => (k, v)}.toStream))
    Stream.continually(Stream(Bulk[K](), Bulk[V]())).flatten.zip(futures)
  }
}

final case class MultiBulkWithScores[A](implicit parse: Parse[A]) extends Handler[Option[Stream[(A, Double)]]] {
  def apply(data: Array[Byte], future: Option[CompletableFuture[Any]]) = {
    val futures = Stream.fill[Option[CompletableFuture[Any]]](string(data).toInt)(if (future.isDefined) Some(new DefaultCompletableFuture[Any](5000)) else None)
    complete(future, Some(futures.collect{case Some(f) => f.await.result}.grouped(2).collect{case Seq(Some(Result(Some(k))), Some(Result(Some(v)))) => (k, v)}.toStream))
    Stream.continually(Stream(Bulk[A](), Bulk[Double]())).flatten.zip(futures)
  }
}

final case class MultiBulkAsFlat[A](implicit parse: Parse[A]) extends Handler[Option[Stream[A]]] {
  def apply(data: Array[Byte], future: Option[CompletableFuture[Any]]) = {
    val futures = Stream.fill[Option[CompletableFuture[Any]]](string(data).toInt)(if (future.isDefined) Some(new DefaultCompletableFuture[Any](5000)) else None)
    complete(future, Some(futures.collect{case Some(f) => f.await.result}.collect{case Some(Result(Some(v))) => v}))
    futures.map(f => (Bulk[A](), f))
  }
}
