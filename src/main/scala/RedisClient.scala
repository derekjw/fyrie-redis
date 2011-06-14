package net.fyrie
package redis

import actors._
import messages.{Request, MultiRequest, Disconnect}
import Protocol.EOL
import types._
import serialization.Parse

import akka.actor.{Actor,ActorRef,IOManager,PoisonPill}
import Actor.{actorOf}
import akka.util.ByteString
import akka.dispatch.{Future, Promise}

import collection.mutable.Queue

object RedisClient {
  def connect(host: String = "localhost", port: Int = 6379, ioManager: ActorRef = actorOf(new IOManager()).start) =
    new RedisClient(host, port, ioManager)
}

class RedisClient(host: String = "localhost", port: Int = 6379, val ioManager: ActorRef = actorOf(new IOManager()).start) extends RedisClientAsync with FlexibleRedisClient {
  client =>

  val actor = actorOf(new RedisClientSession(ioManager, host, port)).start

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

  def multi(block: (RedisClientMulti) => Unit): Unit = {
    val rq = new RedisClientMulti { val actor = client.actor }
    block(rq)
    rq.exec()
  }
}

sealed trait FlexibleRedisClient {

  def sync: RedisClientSync
  def async: RedisClientAsync
  def quiet: RedisClientQuiet

}

trait RedisClientAsync extends Commands {
  type RawResult = Future[Any]
  type Result[A] = Future[A]

  protected def send(in: List[ByteString]): Future[Any] = actor ? Request(format(in))

  protected implicit def resultAsMultiBulk(future: Future[Any]): Future[Option[List[Option[ByteString]]]] = future map toMultiBulk
  protected implicit def resultAsMultiBulkList(future: Future[Any]): Future[List[Option[ByteString]]] = future map toMultiBulkList
  protected implicit def resultAsMultiBulkFlat(future: Future[Any]): Future[Option[List[ByteString]]] = future map toMultiBulkFlat
  protected implicit def resultAsMultiBulkFlatList(future: Future[Any]): Future[List[ByteString]] = future map toMultiBulkFlatList
  protected implicit def resultAsMultiBulkSet(future: Future[Any]): Future[Set[ByteString]] = future map toMultiBulkSet
  protected implicit def resultAsMultiBulkMap(future: Future[Any]): Future[Map[ByteString, ByteString]] = future map toMultiBulkMap
  protected implicit def resultAsMultiBulkScored(future: Future[Any]): Future[List[(ByteString, Double)]] = future map toMultiBulkScored
  protected implicit def resultAsMultiBulkSinglePair(future: Future[Any]): Future[Option[(ByteString, ByteString)]] = future map toMultiBulkSinglePair
  protected implicit def resultAsMultiBulkSinglePairK[K: Parse](future: Future[Any]): Future[Option[(K, ByteString)]] = future map (toMultiBulkSinglePair(_).map(kv => (Parse(kv._1), kv._2)))
  protected implicit def resultAsBulk(future: Future[Any]): Future[Option[ByteString]] = future map toBulk
  protected implicit def resultAsDouble(future: Future[Any]): Future[Double] = future map toDouble
  protected implicit def resultAsDoubleOption(future: Future[Any]): Future[Option[Double]] = future map toDoubleOption
  protected implicit def resultAsLong(future: Future[Any]): Future[Long] = future map toLong
  protected implicit def resultAsInt(future: Future[Any]): Future[Int] = future map (toLong(_).toInt)
  protected implicit def resultAsIntOption(future: Future[Any]): Future[Option[Int]] = future map toIntOption
  protected implicit def resultAsBool(future: Future[Any]): Future[Boolean] = future map toBool
  protected implicit def resultAsStatus(future: Future[Any]): Future[String] = future map toStatus
  protected implicit def resultAsOkStatus(future: Future[Any]): Future[Unit] = future map toOkStatus

}

trait RedisClientSync extends Commands {
  type RawResult = Any
  type Result[A] = A

  protected def send(in: List[ByteString]): Any = (actor ? Request(format(in))).get

  protected implicit def resultAsMultiBulk(raw: Any): Option[List[Option[ByteString]]] = toMultiBulk(raw)
  protected implicit def resultAsMultiBulkList(raw: Any): List[Option[ByteString]] = toMultiBulkList(raw)
  protected implicit def resultAsMultiBulkFlat(raw: Any): Option[List[ByteString]] = toMultiBulkFlat(raw)
  protected implicit def resultAsMultiBulkFlatList(raw: Any): List[ByteString] = toMultiBulkFlatList(raw)
  protected implicit def resultAsMultiBulkSet(raw: Any): Set[ByteString] = toMultiBulkSet(raw)
  protected implicit def resultAsMultiBulkMap(raw: Any): Map[ByteString, ByteString] = toMultiBulkMap(raw)
  protected implicit def resultAsMultiBulkScored(raw: Any): List[(ByteString, Double)] = toMultiBulkScored(raw)
  protected implicit def resultAsMultiBulkSinglePair(raw: Any): Option[(ByteString, ByteString)] = toMultiBulkSinglePair(raw)
  protected implicit def resultAsMultiBulkSinglePairK[K: Parse](raw: Any): Option[(K, ByteString)] = toMultiBulkSinglePair(raw).map(kv => (Parse(kv._1), kv._2))
  protected implicit def resultAsBulk(raw: Any): Option[ByteString] = toBulk(raw)
  protected implicit def resultAsDouble(raw: Any): Double = toDouble(raw)
  protected implicit def resultAsDoubleOption(raw: Any): Option[Double] = toDoubleOption(raw)
  protected implicit def resultAsLong(raw: Any): Long = toLong(raw)
  protected implicit def resultAsInt(raw: Any): Int = toLong(raw).toInt
  protected implicit def resultAsIntOption(raw: Any): Option[Int] = toIntOption(raw)
  protected implicit def resultAsBool(raw: Any): Boolean = toBool(raw)
  protected implicit def resultAsStatus(raw: Any): String = toStatus(raw)
  protected implicit def resultAsOkStatus(raw: Any): Unit = toOkStatus(raw)

}

trait RedisClientQuiet extends Commands {
  type RawResult = Unit
  type Result[_] = Unit

  protected def send(in: List[ByteString]) = actor ! Request(format(in))

  protected implicit def resultAsMultiBulk(raw: Unit): Unit = ()
  protected implicit def resultAsMultiBulkList(raw: Unit): Unit = ()
  protected implicit def resultAsMultiBulkFlat(raw: Unit): Unit = ()
  protected implicit def resultAsMultiBulkFlatList(raw: Unit): Unit = ()
  protected implicit def resultAsMultiBulkSet(raw: Unit): Unit = ()
  protected implicit def resultAsMultiBulkMap(raw: Unit): Unit = ()
  protected implicit def resultAsMultiBulkScored(raw: Unit): Unit = ()
  protected implicit def resultAsMultiBulkSinglePair(raw: Unit): Unit = ()
  protected implicit def resultAsMultiBulkSinglePairK[K: Parse](raw: Unit): Unit = ()
  protected implicit def resultAsBulk(raw: Unit): Unit = ()
  protected implicit def resultAsDouble(raw: Unit): Unit = ()
  protected implicit def resultAsDoubleOption(raw: Unit): Unit = ()
  protected implicit def resultAsLong(raw: Unit): Unit = ()
  protected implicit def resultAsInt(raw: Unit): Unit = ()
  protected implicit def resultAsIntOption(raw: Unit): Unit = ()
  protected implicit def resultAsBool(raw: Unit): Unit = ()
  protected implicit def resultAsStatus(raw: Unit): Unit = ()
  protected implicit def resultAsOkStatus(raw: Unit): Unit = ()
}

trait RedisClientMulti extends Commands {
  type RawResult = Queued[RedisType]
  type Result[A] = Queued[A]

  private object Queued {
    def apply(promise: Promise[RedisType]) = new Queued(promise,promise)
    def complete[A](queued: Queued[A], value: Either[Throwable, RedisType]): Unit = queued.promise.complete(value)
    def isCompleted(queued: Queued[_]): Boolean = queued.promise.isCompleted
  }

  class Queued[A] private (private val promise: Promise[RedisType], private val result: Future[A]) {
    def map[B](f: A => B): Queued[B] = new Queued[B](promise, result map f)
    def <-:(that: Promise[A]): Queued[A] = {
      result.onComplete(f => that.complete(f.value.get))
      this
    }
  }

  private var requests: List[(ByteString, Promise[RedisType])] = Nil
  private val responses: Queue[Queued[RedisType]] = Queue.empty

  protected def send(in: List[ByteString]): Queued[RedisType] = {
    val status = Promise[RedisType]()
    val statusFuture = status map toStatus
    val bytes = format(in)
    requests ::= ((bytes, status))
    val queued = Queued(Promise())
    statusFuture.onComplete(f => if (f.exception.isDefined) Queued.complete(queued, Left(f.exception.get)))
    responses enqueue queued
    queued
  }

  def exec(): Unit = {
    actor ? MultiRequest(format(List(Protocol.MULTI)), requests.reverse, format(List(Protocol.EXEC))) foreach {
      _ match {
        case RedisMulti(m) =>
          for {
            list <- m
            rtype <- list
          } {
            while (Queued.isCompleted(responses.head)) { responses.dequeue }
            Queued.complete(responses.dequeue, Right(rtype))
          }
        case RedisError(e) =>
          val re = RedisErrorException(e)
          while (responses.nonEmpty) { Queued.complete(responses.dequeue, Left(re)) }
        case _ =>
          val re = RedisProtocolException("Unexpected response")
          while (responses.nonEmpty) { Queued.complete(responses.dequeue, Left(re)) }
      }
    }
  }

  protected implicit def resultAsMultiBulk(queued: Queued[RedisType]): Queued[Option[List[Option[ByteString]]]] = queued map toMultiBulk
  protected implicit def resultAsMultiBulkList(queued: Queued[RedisType]): Queued[List[Option[ByteString]]] = queued map toMultiBulkList
  protected implicit def resultAsMultiBulkFlat(queued: Queued[RedisType]): Queued[Option[List[ByteString]]] = queued map toMultiBulkFlat
  protected implicit def resultAsMultiBulkFlatList(queued: Queued[RedisType]): Queued[List[ByteString]] = queued map toMultiBulkFlatList
  protected implicit def resultAsMultiBulkSet(queued: Queued[RedisType]): Queued[Set[ByteString]] = queued map toMultiBulkSet
  protected implicit def resultAsMultiBulkMap(queued: Queued[RedisType]): Queued[Map[ByteString, ByteString]] = queued map toMultiBulkMap
  protected implicit def resultAsMultiBulkScored(queued: Queued[RedisType]): Queued[List[(ByteString, Double)]] = queued map toMultiBulkScored
  protected implicit def resultAsMultiBulkSinglePair(queued: Queued[RedisType]): Queued[Option[(ByteString, ByteString)]] = queued map toMultiBulkSinglePair
  protected implicit def resultAsMultiBulkSinglePairK[K: Parse](queued: Queued[RedisType]): Queued[Option[(K, ByteString)]] = queued map (toMultiBulkSinglePair(_).map(kv => (Parse(kv._1), kv._2)))
  protected implicit def resultAsBulk(queued: Queued[RedisType]): Queued[Option[ByteString]] = queued map toBulk
  protected implicit def resultAsDouble(queued: Queued[RedisType]): Queued[Double] = queued map toDouble
  protected implicit def resultAsDoubleOption(queued: Queued[RedisType]): Queued[Option[Double]] = queued map toDoubleOption
  protected implicit def resultAsLong(queued: Queued[RedisType]): Queued[Long] = queued map toLong
  protected implicit def resultAsInt(queued: Queued[RedisType]): Queued[Int] = queued map (toLong(_).toInt)
  protected implicit def resultAsIntOption(queued: Queued[RedisType]): Queued[Option[Int]] = queued map toIntOption
  protected implicit def resultAsBool(queued: Queued[RedisType]): Queued[Boolean] = queued map toBool
  protected implicit def resultAsStatus(queued: Queued[RedisType]): Queued[String] = queued map toStatus
  protected implicit def resultAsOkStatus(queued: Queued[RedisType]): Queued[Unit] = queued map toOkStatus

}


import commands._
trait Commands extends Keys with Servers with Strings with Lists with Sets with SortedSets with Hashes {
  type RawResult
  type Result[_]

  def actor: ActorRef

  protected def send(in: List[ByteString]): RawResult

  protected def format(in: List[ByteString]): ByteString = {
    var count = 0
    var cmd = ByteString.empty
    in foreach { bytes =>
      count += 1
      cmd ++= ByteString("$"+bytes.length) ++ EOL ++ bytes ++ EOL
    }
    ByteString("*"+count) ++ EOL ++ cmd
  }

  protected implicit def resultAsMultiBulk(raw: RawResult): Result[Option[List[Option[ByteString]]]]
  protected implicit def resultAsMultiBulkList(raw: RawResult): Result[List[Option[ByteString]]]
  protected implicit def resultAsMultiBulkFlat(raw: RawResult): Result[Option[List[ByteString]]]
  protected implicit def resultAsMultiBulkFlatList(raw: RawResult): Result[List[ByteString]]
  protected implicit def resultAsMultiBulkSet(raw: RawResult): Result[Set[ByteString]]
  protected implicit def resultAsMultiBulkMap(raw: RawResult): Result[Map[ByteString, ByteString]]
  protected implicit def resultAsMultiBulkScored(raw: RawResult): Result[List[(ByteString, Double)]]
  protected implicit def resultAsMultiBulkSinglePair(raw: RawResult): Result[Option[(ByteString, ByteString)]]
  protected implicit def resultAsMultiBulkSinglePairK[K: Parse](raw: RawResult): Result[Option[(K, ByteString)]]
  protected implicit def resultAsBulk(raw: RawResult): Result[Option[ByteString]]
  protected implicit def resultAsDouble(raw: RawResult): Result[Double]
  protected implicit def resultAsDoubleOption(raw: RawResult): Result[Option[Double]]
  protected implicit def resultAsLong(raw: RawResult): Result[Long]
  protected implicit def resultAsInt(raw: RawResult): Result[Int]
  protected implicit def resultAsIntOption(raw: RawResult): Result[Option[Int]]
  protected implicit def resultAsBool(raw: RawResult): Result[Boolean]
  protected implicit def resultAsStatus(raw: RawResult): Result[String]
  protected implicit def resultAsOkStatus(raw: RawResult): Result[Unit]

  protected val toMultiBulk: Any => Option[List[Option[ByteString]]] = _ match {
    case RedisMulti(m) => m map (_ map toBulk)
    case RedisError(e) => throw RedisErrorException(e)
    case _ => throw RedisProtocolException("Unexpected response")
  }

  protected val toMultiBulkList: Any => List[Option[ByteString]] = _ match {
    case RedisMulti(Some(m)) => m map toBulk
    case RedisMulti(None) => Nil
    case RedisError(e) => throw RedisErrorException(e)
    case _ => throw RedisProtocolException("Unexpected response")
  }

  protected val toMultiBulkFlat: Any => Option[List[ByteString]] = _ match {
    case RedisMulti(m) => m map (_ flatMap (toBulk(_).toList))
    case RedisError(e) => throw RedisErrorException(e)
    case _ => throw RedisProtocolException("Unexpected response")
  }

  protected val toMultiBulkFlatList: Any => List[ByteString] = _ match {
    case RedisMulti(Some(m)) => m flatMap (toBulk(_).toList)
    case RedisMulti(None) => Nil
    case RedisError(e) => throw RedisErrorException(e)
    case _ => throw RedisProtocolException("Unexpected response")
  }

  protected val toMultiBulkSet: Any => Set[ByteString] = _ match {
    case RedisMulti(Some(m)) => m.flatMap(toBulk(_).toList)(collection.breakOut)
    case RedisMulti(None) => Set.empty
    case RedisError(e) => throw RedisErrorException(e)
    case _ => throw RedisProtocolException("Unexpected response")
  }

  protected val toMultiBulkMap: Any => Map[ByteString, ByteString] = _ match {
    case RedisMulti(Some(m)) => m.grouped(2).collect {
      case List(RedisBulk(Some(a)), RedisBulk(Some(b))) => (a, b)
    } toMap
    case RedisMulti(None) => Map.empty
    case RedisError(e) => throw RedisErrorException(e)
    case _ => throw RedisProtocolException("Unexpected response")
  }

  protected val toMultiBulkScored: Any => List[(ByteString, Double)] = _ match {
    case RedisMulti(Some(m)) => m.grouped(2).collect {
      case List(RedisBulk(Some(a)), RedisBulk(Some(b))) => (a, Parse[Double](b))
    } toList
    case RedisMulti(None) => Nil
    case RedisError(e) => throw RedisErrorException(e)
    case _ => throw RedisProtocolException("Unexpected response")
  }

  protected val toMultiBulkSinglePair: Any => Option[(ByteString, ByteString)] = _ match {
    case RedisMulti(Some(List(RedisBulk(Some(a)),RedisBulk(Some(b))))) => Some((a,b))
    case RedisMulti(_) => None
    case RedisError(e) => throw RedisErrorException(e)
    case _ => throw RedisProtocolException("Unexpected response")
  }

  protected val toBulk: Any => Option[ByteString] = _ match {
    case RedisBulk(b) => b
    case RedisError(e) => throw RedisErrorException(e)
    case _ => throw RedisProtocolException("Unexpected response")
  }

  protected val toDouble: Any => Double = _ match {
    case RedisBulk(Some(b)) => Parse[Double](b)
    case RedisError(e) => throw RedisErrorException(e)
    case _ => throw RedisProtocolException("Unexpected response")
  }

  protected val toDoubleOption: Any => Option[Double] = _ match {
    case RedisBulk(b) => b map (Parse[Double](_))
    case RedisError(e) => throw RedisErrorException(e)
    case _ => throw RedisProtocolException("Unexpected response")
  }

  protected val toLong: Any => Long = _ match {
    case RedisInteger(n) => n
    case RedisError(e) => throw RedisErrorException(e)
    case _ => throw RedisProtocolException("Unexpected response")
  }

  protected val toIntOption: Any => Option[Int] = _ match {
    case RedisInteger(n) => if (n >= 0) Some(n.toInt) else None
    case RedisBulk(None) => None
    case RedisError(e) => throw RedisErrorException(e)
    case _ => throw RedisProtocolException("Unexpected response")
  }

  protected val toBool: Any => Boolean = _ match {
    case RedisInteger(1) => true
    case RedisInteger(0) => false
    case RedisError(e) => throw RedisErrorException(e)
    case _ => throw RedisProtocolException("Unexpected response")
  }

  protected val toStatus: Any => String = _ match {
    case RedisString(s) => s
    case RedisBulk(Some(b)) => Parse[String](b)
    case RedisError(e) => throw RedisErrorException(e)
    case _ => throw RedisProtocolException("Unexpected response")
  }

  protected val toOkStatus: Any => Unit = _ match {
    case RedisString("OK") => Unit
    case RedisError(e) => throw RedisErrorException(e)
    case _ => throw RedisProtocolException("Unexpected response")
  }
}


case class RedisErrorException(message: String) extends RuntimeException(message)
case class RedisProtocolException(message: String) extends RuntimeException(message)
case class RedisConnectionException(message: String) extends RuntimeException(message)
