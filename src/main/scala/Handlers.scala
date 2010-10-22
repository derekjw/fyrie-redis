package net.fyrie.redis
package handlers

import serialization.{ Parse }
import Parse.Implicits._

import se.scalablesolutions.akka.dispatch.{Future, CompletableFuture, DefaultCompletableFuture}

abstract class Handler[A,B] {
  def apply(data: Array[Byte], future: Option[CompletableFuture[Any]]): Seq[(Handler[_,_], Option[CompletableFuture[Any]])]

  def verify(in: String, expect: String): Unit =
    if (in != expect) throw new RedisProtocolException("Expected '" + expect + "' reply, got: " + in)

  def string(in: Array[Byte]): String = new String(in, "UTF-8")

  def complete(future: Option[CompletableFuture[Any]], value: => Any) =
    future foreach { f =>
      try {
        f.completeWithResult(value)
      } catch {
        case e: Exception => f.completeWithException(e)
      }
    }

  def mkFuturePair: (CompletableFuture[Any], Future[B]) = {
    val f = new DefaultCompletableFuture[A](5000)
    (f.asInstanceOf[CompletableFuture[Any]], f.map(parseResult))
  }

  def parseResult(in: A): B
}

final case class MultiExec(handlers: Seq[Handler[_,_]]) extends Handler[Option[Stream[Future[_]]], Option[Stream[_]]] {
  def apply(data: Array[Byte], future: Option[CompletableFuture[Any]]) = {
    OkStatus(data, None)
    val futures: Stream[Option[(CompletableFuture[Any], Future[_])]] =
      handlers.toStream.map{ h => if (future.isDefined) Some(h.mkFuturePair) else None }
    complete(future, Some(futures.flatMap(_.map(_._2))))
    Stream.fill(handlers.length)(QueuedStatus).foldLeft(Stream[(Handler[_,_], Option[CompletableFuture[Any]])]((MultiExecResult(handlers.toStream zip (futures.map(_.map(_._1)))), future))){ case (s, h) => (h, None) #:: s }
  }

  // This should throw an exception if a future contains one
  def parseResult(in: Option[Stream[Future[_]]]): Option[Stream[_]] =
    in.map(_.flatMap(_.await.result))
}

final case class MultiExecResult(hfs: Seq[(Handler[_,_], Option[CompletableFuture[Any]])]) extends Handler[Option[Stream[Future[_]]], Option[Stream[_]]] {
  def apply(data: Array[Byte], future: Option[CompletableFuture[Any]]) = {
    hfs
  }

  def parseResult(in: Option[Stream[Future[_]]]): Option[Stream[_]] =
    in.map(_.map(_.await.result.get))
}

case object NoHandler extends Handler[Unit,Unit] {
  def apply(data: Array[Byte], future: Option[CompletableFuture[Any]]) = {
    future foreach (_.completeWithException( new Exception("Can't handle reply")))
    Nil
  }

  def parseResult(in: Unit): Unit = ()
}

case object Status extends Handler[String,String] {
  def apply(data: Array[Byte], future: Option[CompletableFuture[Any]]) = {
    complete(future, string(data))
    Nil
  }

  def parseResult(in: String): String = in
}

case object OkStatus extends Handler[String,Unit] {
  def apply(data: Array[Byte], future: Option[CompletableFuture[Any]]) = {
    complete(future, string(data))
    Nil
  }

  def parseResult(in: String): Unit = verify(in, "OK")
}

case object QueuedStatus extends Handler[String,Unit] {
  def apply(data: Array[Byte], future: Option[CompletableFuture[Any]]) = {
    complete(future, string(data))
    Nil
  }

  def parseResult(in: String): Unit = verify(in, "QUEUED")
}

case object ShortInt extends Handler[String,Int] {
  def apply(data: Array[Byte], future: Option[CompletableFuture[Any]]) = {
    complete(future, string(data))
    Nil
  }

  def parseResult(in: String): Int = in.toInt
}

case object LongInt extends Handler[String,Long] {
  def apply(data: Array[Byte], future: Option[CompletableFuture[Any]]) = {
    complete(future, string(data))
    Nil
  }

  def parseResult(in: String): Long = in.toLong
}

case object IntAsBoolean extends Handler[String,Boolean] {
  def apply(data: Array[Byte], future: Option[CompletableFuture[Any]]) = {
    complete(future, string(data))
    Nil
  }

  def parseResult(in: String): Boolean = in.toInt > 0
}

final case class Bulk[A](implicit parse: Parse[A]) extends Handler[Option[Array[Byte]],Option[A]] {
  def apply(data: Array[Byte], future: Option[CompletableFuture[Any]]) = {
    complete(future, Some(data))
    Nil
  }

  def parseResult(in: Option[Array[Byte]]): Option[A] = in.map(parse)
}

final case class MultiBulk[A](implicit parse: Parse[A]) extends Handler[Option[Stream[Future[Option[Array[Byte]]]]], Option[Stream[Option[A]]]] {
  def apply(data: Array[Byte], future: Option[CompletableFuture[Any]]) = {
    val futures = Stream.fill[Option[CompletableFuture[Any]]](string(data).toInt)(if (future.isDefined) Some(new DefaultCompletableFuture[Any](5000)) else None)
    complete(future, Some(futures.flatten))
    futures.map(f => (Bulk[A](), f))
  }

  def parseResult(in: Option[Stream[Future[Option[Array[Byte]]]]]): Option[Stream[Option[A]]] =
    in.map(_.map(_.await.result.flatMap(_.map(parse))))
}

final case class MultiBulkAsPairs[K, V](implicit parseK: Parse[K], parseV: Parse[V]) extends Handler[Option[Stream[Future[Option[Array[Byte]]]]], Option[Stream[(K, V)]]] {
  def apply(data: Array[Byte], future: Option[CompletableFuture[Any]]) = {
    val futures = Stream.fill[Option[CompletableFuture[Any]]](string(data).toInt)(if (future.isDefined) Some(new DefaultCompletableFuture[Any](5000)) else None)
    complete(future, Some(futures.flatten))
    Stream.continually(Stream(Bulk[K](), Bulk[V]())).flatten.zip(futures)
  }

  def parseResult(in: Option[Stream[Future[Option[Array[Byte]]]]]): Option[Stream[(K,V)]] =
    in.map(_.map(_.await.result.get).grouped(2).toStream.flatMap{
      case Stream(Some(k), Some(v)) => Some(parseK(k), parseV(v))
      case _ => None
    })
}

final case class MultiBulkWithScores[A](implicit parse: Parse[A]) extends Handler[Option[Stream[Future[Option[Array[Byte]]]]], Option[Stream[(A, Double)]]] {
  def apply(data: Array[Byte], future: Option[CompletableFuture[Any]]) = {
    val futures = Stream.fill[Option[CompletableFuture[Any]]](string(data).toInt)(if (future.isDefined) Some(new DefaultCompletableFuture[Any](5000)) else None)
    complete(future, Some(futures.flatten))
    Stream.continually(Stream(Bulk[A](), Bulk[Double]())).flatten.zip(futures)
  }

  def parseResult(in: Option[Stream[Future[Option[Array[Byte]]]]]): Option[Stream[(A, Double)]] =
    in.map(_.map(_.await.result.get).grouped(2).toStream.flatMap{
      case Stream(Some(k), Some(v)) => Some(parse(k), parseDouble(v))
      case _ => None
    })

}

final case class MultiBulkAsFlat[A](implicit parse: Parse[A]) extends Handler[Option[Stream[Future[Option[Array[Byte]]]]],Option[Stream[A]]] {
  def apply(data: Array[Byte], future: Option[CompletableFuture[Any]]) = {
    val futures = Stream.fill[Option[CompletableFuture[Any]]](string(data).toInt)(if (future.isDefined) Some(new DefaultCompletableFuture[Any](5000)) else None)
    complete(future, Some(futures.flatten))
    futures.map(f => (Bulk[A](), f))
  }

  def parseResult(in: Option[Stream[Future[Option[Array[Byte]]]]]): Option[Stream[A]] =
    in.map(_.flatMap(_.await.result.get).map(parse))

}
