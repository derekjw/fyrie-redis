package net.fyrie
package redis

import actors._
import messages.{ Request, MultiRequest, Disconnect }
import Protocol.EOL
import types._
import serialization.Parse

import akka.actor.{ Actor, ActorRef, IOManager, PoisonPill, Timeout }
import Actor.{ actorOf }
import akka.util.ByteString
import akka.dispatch.{ Future, Promise }

case class RedisClientConfig(timeout: Timeout = implicitly,
  autoReconnect: Boolean = true,
  retryOnReconnect: Boolean = true)

object RedisClient {
  def connect(host: String = "localhost", port: Int = 6379, config: RedisClientConfig = RedisClientConfig(), ioManager: ActorRef = actorOf(new IOManager()).start) =
    new RedisClient(host, port, config, ioManager)
}

final class RedisClient(val host: String = "localhost", val port: Int = 6379, val config: RedisClientConfig = RedisClientConfig(), val ioManager: ActorRef = actorOf(new IOManager()).start) extends RedisClientAsync with FlexibleRedisClient {
  client =>

  final protected val actor = actorOf(new RedisClientSession(ioManager, host, port, config)).start

  def disconnect = actor ! Disconnect

  val async = this

  val sync: RedisClientSync = new RedisClientSync with FlexibleRedisClient {
    val actor = client.actor

    val async: RedisClientAsync = client
    val sync: RedisClientSync = this
    val quiet: RedisClientQuiet = client.quiet
  }

  val quiet: RedisClientQuiet = new RedisClientQuiet with FlexibleRedisClient {
    val actor = client.actor

    val async: RedisClientAsync = client
    val sync: RedisClientSync = client.sync
    val quiet: RedisClientQuiet = this
  }

  def multi[T](block: (RedisClientMulti) => Queued[T]): T = {
    val rq = new RedisClientMulti { val actor = client.actor }
    rq.exec(block(rq))
  }
}

sealed trait FlexibleRedisClient {

  def sync: RedisClientSync
  def async: RedisClientAsync
  def quiet: RedisClientQuiet

}

sealed trait RedisClientAsync extends Commands {
  type Result[A] = Future[A]

  final protected def send(in: List[ByteString]): Future[Any] = actor ? Request(format(in))

  final protected def mapResult[A, B](result: Result[A], f: A => B) = result map f
}

sealed trait RedisClientSync extends Commands {
  type Result[A] = A

  final protected def send(in: List[ByteString]): Any = (actor ? Request(format(in))).get

  final protected def mapResult[A, B](result: Result[A], f: A => B) = f(result)
}

sealed trait RedisClientQuiet extends Commands {
  type Result[_] = Unit

  final protected def send(in: List[ByteString]) = actor ! Request(format(in))

  final protected def mapResult[A, B](result: Result[A], f: A => B) = ()
}

object Queued {
  def apply[A](value: A, request: (ByteString, Promise[RedisType]), response: Promise[RedisType]): Queued[A] =
    new QueuedSingle(value, request, response)
  def apply[A](value: A): Queued[A] = new QueuedValue(value)

  final class QueuedValue[+A](val value: A) extends Queued[A] {
    def requests = Vector.empty
    def responses = Vector.empty
    def flatMap[B](f: A => Queued[B]): Queued[B] = f(value)
    def map[B](f: A => B): Queued[B] = new QueuedValue(f(value))
  }

  final class QueuedSingle[+A](val value: A, val request: (ByteString, Promise[RedisType]), val response: Promise[RedisType]) extends Queued[A] {
    def requests = Vector(request)
    def responses = Vector(response)
    def flatMap[B](f: A => Queued[B]): Queued[B] = {
      val that = f(value)
      new QueuedList(that.value, request +: that.requests, response +: that.responses)
    }
    def map[B](f: A => B): Queued[B] = new QueuedSingle(f(value), request, response)
  }

  final class QueuedList[+A](val value: A, val requests: Vector[(ByteString, Promise[RedisType])], val responses: Vector[Promise[RedisType]]) extends Queued[A] {
    def flatMap[B](f: A => Queued[B]): Queued[B] = {
      f(value) match {
        case that: QueuedList[_] =>
          new QueuedList(that.value, requests ++ that.requests, responses ++ that.responses)
        case that: QueuedSingle[_] =>
          new QueuedList(that.value, requests :+ that.request, responses :+ that.response)
        case that: QueuedValue[_] =>
          new QueuedList(that.value, requests, responses)
      }
    }
    def map[B](f: A => B): Queued[B] = new QueuedList(f(value), requests, responses)
  }
}

sealed trait Queued[+A] {
  def value: A
  def requests: Vector[(ByteString, Promise[RedisType])]
  def responses: Vector[Promise[RedisType]]
  def flatMap[B](f: A => Queued[B]): Queued[B]
  def map[B](f: A => B): Queued[B]
}

sealed trait RedisClientMulti extends Commands {
  type Result[A] = Queued[Future[A]]

  final protected def send(in: List[ByteString]): Queued[Future[Any]] = {
    val status = Promise[RedisType]()
    val statusFuture = status map toStatus
    val bytes = format(in)
    val result = Promise[RedisType]()
    statusFuture onException { case e => result complete Left(e) }
    Queued(result, (bytes, status), result)
  }

  def exec[T](q: Queued[T]): T = {
    actor ? MultiRequest(format(List(Protocol.MULTI)), q.requests, format(List(Protocol.EXEC))) foreach {
      case RedisMulti(m) =>
        var responses = q.responses
        for {
          list <- m
          rtype <- list
        } {
          while (responses.head.isCompleted) { responses = responses.tail }
          responses.head complete Right(rtype)
          responses = responses.tail
        }
      case RedisError(e) =>
        val re = RedisErrorException(e)
        q.responses foreach (_.complete(Left(re)))
      case _ =>
        val re = RedisProtocolException("Unexpected response")
        q.responses foreach (_.complete(Left(re)))
    }
    q.value
  }

  final protected def mapResult[A, B](result: Result[A], f: A => B) = result map (_ map f)

}

import commands._
sealed trait Commands extends Keys with Servers with Strings with Lists with Sets with SortedSets with Hashes {
  protected type Result[_]

  protected def actor: ActorRef

  protected def send(in: List[ByteString]): Result[Any]

  final protected def format(in: List[ByteString]): ByteString = {
    var count = 0
    var cmd = ByteString.empty
    in foreach { bytes =>
      count += 1
      cmd ++= ByteString("$" + bytes.length) ++ EOL ++ bytes ++ EOL
    }
    ByteString("*" + count) ++ EOL ++ cmd
  }

  protected def mapResult[A, B](result: Result[A], f: A => B): Result[B]

  final protected implicit def resultAsMultiBulk(raw: Result[Any]): Result[Option[List[Option[ByteString]]]] = mapResult(raw, toMultiBulk)
  final protected implicit def resultAsMultiBulkList(raw: Result[Any]): Result[List[Option[ByteString]]] = mapResult(raw, toMultiBulkList)
  final protected implicit def resultAsMultiBulkFlat(raw: Result[Any]): Result[Option[List[ByteString]]] = mapResult(raw, toMultiBulkFlat)
  final protected implicit def resultAsMultiBulkFlatList(raw: Result[Any]): Result[List[ByteString]] = mapResult(raw, toMultiBulkFlatList)
  final protected implicit def resultAsMultiBulkSet(raw: Result[Any]): Result[Set[ByteString]] = mapResult(raw, toMultiBulkSet)
  final protected implicit def resultAsMultiBulkMap(raw: Result[Any]): Result[Map[ByteString, ByteString]] = mapResult(raw, toMultiBulkMap)
  final protected implicit def resultAsMultiBulkScored(raw: Result[Any]): Result[List[(ByteString, Double)]] = mapResult(raw, toMultiBulkScored)
  final protected implicit def resultAsMultiBulkSinglePair(raw: Result[Any]): Result[Option[(ByteString, ByteString)]] = mapResult(raw, toMultiBulkSinglePair)
  final protected implicit def resultAsMultiBulkSinglePairK[K: Parse](raw: Result[Any]): Result[Option[(K, ByteString)]] = mapResult(raw, toMultiBulkSinglePair(_: Any).map(kv => (Parse(kv._1), kv._2)))
  final protected implicit def resultAsBulk(raw: Result[Any]): Result[Option[ByteString]] = mapResult(raw, toBulk)
  final protected implicit def resultAsDouble(raw: Result[Any]): Result[Double] = mapResult(raw, toDouble)
  final protected implicit def resultAsDoubleOption(raw: Result[Any]): Result[Option[Double]] = mapResult(raw, toDoubleOption)
  final protected implicit def resultAsLong(raw: Result[Any]): Result[Long] = mapResult(raw, toLong)
  final protected implicit def resultAsInt(raw: Result[Any]): Result[Int] = mapResult(raw, toLong(_: Any).toInt)
  final protected implicit def resultAsIntOption(raw: Result[Any]): Result[Option[Int]] = mapResult(raw, toIntOption)
  final protected implicit def resultAsBool(raw: Result[Any]): Result[Boolean] = mapResult(raw, toBool)
  final protected implicit def resultAsStatus(raw: Result[Any]): Result[String] = mapResult(raw, toStatus)
  final protected implicit def resultAsOkStatus(raw: Result[Any]): Result[Unit] = mapResult(raw, toOkStatus)

  final protected val toMultiBulk: Any => Option[List[Option[ByteString]]] = _ match {
    case RedisMulti(m) => m map (_ map toBulk)
    case RedisError(e) => throw RedisErrorException(e)
    case _ => throw RedisProtocolException("Unexpected response")
  }

  final protected val toMultiBulkList: Any => List[Option[ByteString]] = _ match {
    case RedisMulti(Some(m)) => m map toBulk
    case RedisMulti(None) => Nil
    case RedisError(e) => throw RedisErrorException(e)
    case _ => throw RedisProtocolException("Unexpected response")
  }

  final protected val toMultiBulkFlat: Any => Option[List[ByteString]] = _ match {
    case RedisMulti(m) => m map (_ flatMap (toBulk(_).toList))
    case RedisError(e) => throw RedisErrorException(e)
    case _ => throw RedisProtocolException("Unexpected response")
  }

  final protected val toMultiBulkFlatList: Any => List[ByteString] = _ match {
    case RedisMulti(Some(m)) => m flatMap (toBulk(_).toList)
    case RedisMulti(None) => Nil
    case RedisError(e) => throw RedisErrorException(e)
    case _ => throw RedisProtocolException("Unexpected response")
  }

  final protected val toMultiBulkSet: Any => Set[ByteString] = _ match {
    case RedisMulti(Some(m)) => m.flatMap(toBulk(_).toList)(collection.breakOut)
    case RedisMulti(None) => Set.empty
    case RedisError(e) => throw RedisErrorException(e)
    case _ => throw RedisProtocolException("Unexpected response")
  }

  final protected val toMultiBulkMap: Any => Map[ByteString, ByteString] = _ match {
    case RedisMulti(Some(m)) => m.grouped(2).collect {
      case List(RedisBulk(Some(a)), RedisBulk(Some(b))) => (a, b)
    } toMap
    case RedisMulti(None) => Map.empty
    case RedisError(e) => throw RedisErrorException(e)
    case _ => throw RedisProtocolException("Unexpected response")
  }

  final protected val toMultiBulkScored: Any => List[(ByteString, Double)] = _ match {
    case RedisMulti(Some(m)) => m.grouped(2).collect {
      case List(RedisBulk(Some(a)), RedisBulk(Some(b))) => (a, Parse[Double](b))
    } toList
    case RedisMulti(None) => Nil
    case RedisError(e) => throw RedisErrorException(e)
    case _ => throw RedisProtocolException("Unexpected response")
  }

  final protected val toMultiBulkSinglePair: Any => Option[(ByteString, ByteString)] = _ match {
    case RedisMulti(Some(List(RedisBulk(Some(a)), RedisBulk(Some(b))))) => Some((a, b))
    case RedisMulti(_) => None
    case RedisError(e) => throw RedisErrorException(e)
    case _ => throw RedisProtocolException("Unexpected response")
  }

  final protected val toBulk: Any => Option[ByteString] = _ match {
    case RedisBulk(b) => b
    case RedisError(e) => throw RedisErrorException(e)
    case _ => throw RedisProtocolException("Unexpected response")
  }

  final protected val toDouble: Any => Double = _ match {
    case RedisBulk(Some(b)) => Parse[Double](b)
    case RedisError(e) => throw RedisErrorException(e)
    case _ => throw RedisProtocolException("Unexpected response")
  }

  final protected val toDoubleOption: Any => Option[Double] = _ match {
    case RedisBulk(b) => b map (Parse[Double](_))
    case RedisError(e) => throw RedisErrorException(e)
    case _ => throw RedisProtocolException("Unexpected response")
  }

  final protected val toLong: Any => Long = _ match {
    case RedisInteger(n) => n
    case RedisError(e) => throw RedisErrorException(e)
    case _ => throw RedisProtocolException("Unexpected response")
  }

  final protected val toIntOption: Any => Option[Int] = _ match {
    case RedisInteger(n) => if (n >= 0) Some(n.toInt) else None
    case RedisBulk(None) => None
    case RedisError(e) => throw RedisErrorException(e)
    case _ => throw RedisProtocolException("Unexpected response")
  }

  final protected val toBool: Any => Boolean = _ match {
    case RedisInteger(1) => true
    case RedisInteger(0) => false
    case RedisError(e) => throw RedisErrorException(e)
    case _ => throw RedisProtocolException("Unexpected response")
  }

  final protected val toStatus: Any => String = _ match {
    case RedisString(s) => s
    case RedisBulk(Some(b)) => Parse[String](b)
    case RedisError(e) => throw RedisErrorException(e)
    case _ => throw RedisProtocolException("Unexpected response")
  }

  final protected val toOkStatus: Any => Unit = _ match {
    case RedisString("OK") => Unit
    case RedisError(e) => throw RedisErrorException(e)
    case _ => throw RedisProtocolException("Unexpected response")
  }
}

case class RedisErrorException(message: String) extends RuntimeException(message)
case class RedisProtocolException(message: String) extends RuntimeException(message)
case class RedisConnectionException(message: String) extends RuntimeException(message)
