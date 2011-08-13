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

final class RedisClient(val host: String = "localhost", val port: Int = 6379, val config: RedisClientConfig = RedisClientConfig(), val ioManager: ActorRef = actorOf(new IOManager()).start) extends RedisClientAsync {

  protected val actor = actorOf(new RedisClientSession(ioManager, host, port, config)).start

  def disconnect = actor ! Disconnect

  val sync: RedisClientSync = new RedisClientSync {
    val actor = RedisClient.this.actor

    val async: RedisClientAsync = RedisClient.this
    val quiet: RedisClientQuiet = RedisClient.this.quiet
  }

  val quiet: RedisClientQuiet = new RedisClientQuiet {
    val actor = RedisClient.this.actor

    val async: RedisClientAsync = RedisClient.this
    val sync: RedisClientSync = RedisClient.this.sync
  }

  def multi[T](block: (RedisClientMulti) => Queued[T]): T = {
    val rq = new RedisClientMulti { val actor = RedisClient.this.actor }
    rq.exec(block(rq))
  }
}

sealed trait ConfigurableRedisClient {

  def sync: RedisClientSync
  def async: RedisClientAsync
  def quiet: RedisClientQuiet

}

sealed abstract class RedisClientAsync extends Commands[Future] with ConfigurableRedisClient {
  final protected def send(in: List[ByteString]): Future[Any] = actor ? Request(format(in))

  val async: RedisClientAsync = this
}

sealed abstract class RedisClientSync extends Commands[({ type λ[α] = α })#λ] with ConfigurableRedisClient {
  final protected def send(in: List[ByteString]): Any = (actor ? Request(format(in))).get

  val sync: RedisClientSync = this
}

sealed abstract class RedisClientQuiet extends Commands[({ type λ[_] = Unit })#λ] with ConfigurableRedisClient {
  final protected def send(in: List[ByteString]) = actor ! Request(format(in))

  val quiet: RedisClientQuiet = this
}

sealed abstract class RedisClientMulti extends Commands[({ type λ[α] = Queued[Future[α]] })#λ]()(ResultFunctor.multi) {
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
}

import commands._
sealed abstract class Commands[Result[_]](implicit rf: ResultFunctor[Result]) extends Keys[Result] with Servers[Result] with Strings[Result] with Lists[Result] with Sets[Result] with SortedSets[Result] with Hashes[Result] {

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

  final protected implicit def resultAsMultiBulk(raw: Result[Any]): Result[Option[List[Option[ByteString]]]] = rf.fmap(raw)(toMultiBulk)
  final protected implicit def resultAsMultiBulkList(raw: Result[Any]): Result[List[Option[ByteString]]] = rf.fmap(raw)(toMultiBulkList)
  final protected implicit def resultAsMultiBulkFlat(raw: Result[Any]): Result[Option[List[ByteString]]] = rf.fmap(raw)(toMultiBulkFlat)
  final protected implicit def resultAsMultiBulkFlatList(raw: Result[Any]): Result[List[ByteString]] = rf.fmap(raw)(toMultiBulkFlatList)
  final protected implicit def resultAsMultiBulkSet(raw: Result[Any]): Result[Set[ByteString]] = rf.fmap(raw)(toMultiBulkSet)
  final protected implicit def resultAsMultiBulkMap(raw: Result[Any]): Result[Map[ByteString, ByteString]] = rf.fmap(raw)(toMultiBulkMap)
  final protected implicit def resultAsMultiBulkScored(raw: Result[Any]): Result[List[(ByteString, Double)]] = rf.fmap(raw)(toMultiBulkScored)
  final protected implicit def resultAsMultiBulkSinglePair(raw: Result[Any]): Result[Option[(ByteString, ByteString)]] = rf.fmap(raw)(toMultiBulkSinglePair)
  final protected implicit def resultAsMultiBulkSinglePairK[K: Parse](raw: Result[Any]): Result[Option[(K, ByteString)]] = rf.fmap(raw)(toMultiBulkSinglePair(_).map(kv => (Parse(kv._1), kv._2)))
  final protected implicit def resultAsBulk(raw: Result[Any]): Result[Option[ByteString]] = rf.fmap(raw)(toBulk)
  final protected implicit def resultAsDouble(raw: Result[Any]): Result[Double] = rf.fmap(raw)(toDouble)
  final protected implicit def resultAsDoubleOption(raw: Result[Any]): Result[Option[Double]] = rf.fmap(raw)(toDoubleOption)
  final protected implicit def resultAsLong(raw: Result[Any]): Result[Long] = rf.fmap(raw)(toLong)
  final protected implicit def resultAsInt(raw: Result[Any]): Result[Int] = rf.fmap(raw)(toLong(_).toInt)
  final protected implicit def resultAsIntOption(raw: Result[Any]): Result[Option[Int]] = rf.fmap(raw)(toIntOption)
  final protected implicit def resultAsBool(raw: Result[Any]): Result[Boolean] = rf.fmap(raw)(toBool)
  final protected implicit def resultAsStatus(raw: Result[Any]): Result[String] = rf.fmap(raw)(toStatus)
  final protected implicit def resultAsOkStatus(raw: Result[Any]): Result[Unit] = rf.fmap(raw)(toOkStatus)

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
