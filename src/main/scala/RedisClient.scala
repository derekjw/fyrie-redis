package net.fyrie
package redis

import actors._
import messages.{Request}
import Protocol.EOL
import types._
import serialization.Parse

import akka.actor.{Actor,ActorRef,IOManager}
import Actor.{actorOf}
import akka.util.ByteString
import akka.dispatch.Future

object RedisClient {
  def connect(host: String = "localhost", port: Int = 6379, ioManager: ActorRef = actorOf(new IOManager()).start) =
    new RedisClient(host, port, ioManager)
}

class RedisClient(host: String = "localhost", port: Int = 6379, val ioManager: ActorRef = actorOf(new IOManager()).start) extends RedisClientAsync {
  client =>

  val actor = actorOf(new RedisClientSession(ioManager, host, port)).start

  val async = this

  val sync: RedisClientSync = new RedisClientSync {
    val actor = client.actor

    val async: RedisClientAsync = client
    val sync: RedisClientSync = this
    val quiet: RedisClientQuiet = client.quiet
  }

  val quiet: RedisClientQuiet = new RedisClientQuiet {
    val actor = client.actor

    val async: RedisClientAsync = client
    val sync: RedisClientSync = client.sync
    val quiet: RedisClientQuiet = this
  }
}

trait RedisClientAsync extends AbstractRedisClient {
  type RawResult = Future[RedisType]
  type Result[A] = Future[A]

  protected def send(in: List[ByteString]): Future[RedisType] = actor !!! Request(format(in))

  protected implicit def resultAsMultiBulk(future: Future[RedisType]): Future[Option[List[Option[ByteString]]]] = future map toMultiBulk
  protected implicit def resultAsMultiBulkList(future: Future[RedisType]): Future[List[Option[ByteString]]] = future map toMultiBulkList
  protected implicit def resultAsMultiBulkFlat(future: Future[RedisType]): Future[Option[List[ByteString]]] = future map toMultiBulkFlat
  protected implicit def resultAsMultiBulkFlatList(future: Future[RedisType]): Future[List[ByteString]] = future map toMultiBulkFlatList
  protected implicit def resultAsMultiBulkSet(future: Future[RedisType]): Future[Set[ByteString]] = future map toMultiBulkSet
  protected implicit def resultAsMultiBulkMap(future: Future[RedisType]): Future[Map[ByteString, ByteString]] = future map toMultiBulkMap
  protected implicit def resultAsMultiBulkScored(future: Future[RedisType]): Future[List[(ByteString, Double)]] = future map toMultiBulkScored
  protected implicit def resultAsMultiBulkSinglePair(future: Future[RedisType]): Future[Option[(ByteString, ByteString)]] = future map toMultiBulkSinglePair
  protected implicit def resultAsMultiBulkSinglePairK[K: Parse](future: Future[RedisType]): Future[Option[(K, ByteString)]] = future map (toMultiBulkSinglePair(_).map(kv => (Parse(kv._1), kv._2)))
  protected implicit def resultAsBulk(future: Future[RedisType]): Future[Option[ByteString]] = future map toBulk
  protected implicit def resultAsDouble(future: Future[RedisType]): Future[Double] = future map toDouble
  protected implicit def resultAsDoubleOption(future: Future[RedisType]): Future[Option[Double]] = future map toDoubleOption
  protected implicit def resultAsLong(future: Future[RedisType]): Future[Long] = future map toLong
  protected implicit def resultAsInt(future: Future[RedisType]): Future[Int] = future map (toLong(_).toInt)
  protected implicit def resultAsIntOption(future: Future[RedisType]): Future[Option[Int]] = future map toIntOption
  protected implicit def resultAsBool(future: Future[RedisType]): Future[Boolean] = future map toBool
  protected implicit def resultAsStatus(future: Future[RedisType]): Future[String] = future map toStatus
  protected implicit def resultAsOkStatus(future: Future[RedisType]): Future[Unit] = future map toOkStatus

}

trait RedisClientSync extends AbstractRedisClient {
  type RawResult = RedisType
  type Result[A] = A

  protected def send(in: List[ByteString]): RedisType = (actor !!! Request(format(in))).get

  protected implicit def resultAsMultiBulk(raw: RedisType): Option[List[Option[ByteString]]] = toMultiBulk(raw)
  protected implicit def resultAsMultiBulkList(raw: RedisType): List[Option[ByteString]] = toMultiBulkList(raw)
  protected implicit def resultAsMultiBulkFlat(raw: RedisType): Option[List[ByteString]] = toMultiBulkFlat(raw)
  protected implicit def resultAsMultiBulkFlatList(raw: RedisType): List[ByteString] = toMultiBulkFlatList(raw)
  protected implicit def resultAsMultiBulkSet(raw: RedisType): Set[ByteString] = toMultiBulkSet(raw)
  protected implicit def resultAsMultiBulkMap(raw: RedisType): Map[ByteString, ByteString] = toMultiBulkMap(raw)
  protected implicit def resultAsMultiBulkScored(raw: RedisType): List[(ByteString, Double)] = toMultiBulkScored(raw)
  protected implicit def resultAsMultiBulkSinglePair(raw: RedisType): Option[(ByteString, ByteString)] = toMultiBulkSinglePair(raw)
  protected implicit def resultAsMultiBulkSinglePairK[K: Parse](raw: RedisType): Option[(K, ByteString)] = toMultiBulkSinglePair(raw).map(kv => (Parse(kv._1), kv._2))
  protected implicit def resultAsBulk(raw: RedisType): Option[ByteString] = toBulk(raw)
  protected implicit def resultAsDouble(raw: RedisType): Double = toDouble(raw)
  protected implicit def resultAsDoubleOption(raw: RedisType): Option[Double] = toDoubleOption(raw)
  protected implicit def resultAsLong(raw: RedisType): Long = toLong(raw)
  protected implicit def resultAsInt(raw: RedisType): Int = toLong(raw).toInt
  protected implicit def resultAsIntOption(raw: RedisType): Option[Int] = toIntOption(raw)
  protected implicit def resultAsBool(raw: RedisType): Boolean = toBool(raw)
  protected implicit def resultAsStatus(raw: RedisType): String = toStatus(raw)
  protected implicit def resultAsOkStatus(raw: RedisType): Unit = toOkStatus(raw)

}

trait RedisClientQuiet extends AbstractRedisClient {
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

trait AbstractRedisClient extends Commands {

  def actor: ActorRef
  def sync: RedisClientSync
  def async: RedisClientAsync
  def quiet: RedisClientQuiet

  protected def format(in: List[ByteString]): ByteString = {
    var count = 0
    var cmd = ByteString.empty
    in foreach { bytes =>
      count += 1
      cmd ++= ByteString("$"+bytes.length) ++ EOL ++ bytes ++ EOL
    }
    ByteString("*"+count) ++ EOL ++ cmd
  }

}

import commands._
trait Commands extends Keys with Servers with Strings with Lists with Sets with SortedSets with Hashes {
  type RawResult
  type Result[_]

  protected def send(in: List[ByteString]): RawResult

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

  protected val toMultiBulk: RedisType => Option[List[Option[ByteString]]] = _ match {
      case RedisMulti(m) => m map (_ map toBulk)
      case RedisError(e) => throw RedisErrorException(e)
      case _ => throw RedisProtocolException("Unexpected response")
    }

  protected val toMultiBulkList: RedisType => List[Option[ByteString]] = _ match {
      case RedisMulti(Some(m)) => m map toBulk
      case RedisMulti(None) => Nil
      case RedisError(e) => throw RedisErrorException(e)
      case _ => throw RedisProtocolException("Unexpected response")
    }

  protected val toMultiBulkFlat: RedisType => Option[List[ByteString]] = _ match {
      case RedisMulti(m) => m map (_ flatMap (toBulk(_).toList))
      case RedisError(e) => throw RedisErrorException(e)
      case _ => throw RedisProtocolException("Unexpected response")
    }

  protected val toMultiBulkFlatList: RedisType => List[ByteString] = _ match {
      case RedisMulti(Some(m)) => m flatMap (toBulk(_).toList)
      case RedisMulti(None) => Nil
      case RedisError(e) => throw RedisErrorException(e)
      case _ => throw RedisProtocolException("Unexpected response")
    }

  protected val toMultiBulkSet: RedisType => Set[ByteString] = _ match {
      case RedisMulti(Some(m)) => m.flatMap(toBulk(_).toList)(collection.breakOut)
      case RedisMulti(None) => Set.empty
      case RedisError(e) => throw RedisErrorException(e)
      case _ => throw RedisProtocolException("Unexpected response")
    }

  protected val toMultiBulkMap: RedisType => Map[ByteString, ByteString] = _ match {
      case RedisMulti(Some(m)) => m.grouped(2).collect {
        case List(RedisBulk(Some(a)), RedisBulk(Some(b))) => (a, b)
      } toMap
      case RedisMulti(None) => Map.empty
      case RedisError(e) => throw RedisErrorException(e)
      case _ => throw RedisProtocolException("Unexpected response")
    }

  protected val toMultiBulkScored: RedisType => List[(ByteString, Double)] = _ match {
      case RedisMulti(Some(m)) => m.grouped(2).collect {
        case List(RedisBulk(Some(a)), RedisBulk(Some(b))) => (a, Parse[Double](b))
      } toList
      case RedisMulti(None) => Nil
      case RedisError(e) => throw RedisErrorException(e)
      case _ => throw RedisProtocolException("Unexpected response")
    }

  protected val toMultiBulkSinglePair: RedisType => Option[(ByteString, ByteString)] = _ match {
      case RedisMulti(Some(List(RedisBulk(Some(a)),RedisBulk(Some(b))))) => Some((a,b))
      case RedisMulti(_) => None
      case RedisError(e) => throw RedisErrorException(e)
      case _ => throw RedisProtocolException("Unexpected response")
    }

  protected val toBulk: RedisType => Option[ByteString] = _ match {
      case RedisBulk(b) => b
      case RedisError(e) => throw RedisErrorException(e)
      case _ => throw RedisProtocolException("Unexpected response")
    }

  protected val toDouble: RedisType => Double = _ match {
      case RedisBulk(Some(b)) => Parse[Double](b)
      case RedisError(e) => throw RedisErrorException(e)
      case _ => throw RedisProtocolException("Unexpected response")
    }

  protected val toDoubleOption: RedisType => Option[Double] = _ match {
      case RedisBulk(b) => b map (Parse[Double](_))
      case RedisError(e) => throw RedisErrorException(e)
      case _ => throw RedisProtocolException("Unexpected response")
    }

  protected val toLong: RedisType => Long = _ match {
      case RedisInteger(n) => n
      case RedisError(e) => throw RedisErrorException(e)
      case _ => throw RedisProtocolException("Unexpected response")
    }

  protected val toIntOption: RedisType => Option[Int] = _ match {
      case RedisInteger(n) => if (n >= 0) Some(n.toInt) else None
      case RedisBulk(None) => None
      case RedisError(e) => throw RedisErrorException(e)
      case _ => throw RedisProtocolException("Unexpected response")
    }

  protected val toBool: RedisType => Boolean = _ match {
      case RedisInteger(1) => true
      case RedisInteger(0) => false
      case RedisError(e) => throw RedisErrorException(e)
      case _ => throw RedisProtocolException("Unexpected response")
    }

  protected val toStatus: RedisType => String = _ match {
      case RedisString(s) => s
      case RedisBulk(Some(b)) => Parse[String](b)
      case RedisError(e) => throw RedisErrorException(e)
      case _ => throw RedisProtocolException("Unexpected response")
    }

  protected val toOkStatus: RedisType => Unit = _ match {
      case RedisString("OK") => Unit
      case RedisError(e) => throw RedisErrorException(e)
      case _ => throw RedisProtocolException("Unexpected response")
    }
}


case class RedisErrorException(message: String) extends RuntimeException(message)
case class RedisProtocolException(message: String) extends RuntimeException(message)
case class RedisConnectionException(message: String) extends RuntimeException(message)
