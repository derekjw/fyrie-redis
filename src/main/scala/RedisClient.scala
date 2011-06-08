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

class RedisClient(host: String = "localhost", port: Int = 6379, val ioManager: ActorRef = actorOf(new IOManager()).start) extends RedisClientAsync {
  client =>

  val actor = actorOf(new RedisClientSession(ioManager, host, port)).start

  val sync = new RedisClientSync {
    val actor = client.actor
  }

  val quiet = new RedisClientQuiet {
    val actor = client.actor
  }
}

trait RedisClientAsync extends Commands {
  type RawResult = Future[RedisType]
  type Result[A] = Future[A]

  protected def send(in: List[ByteString]): Future[RedisType] = actor !!! Request(format(in))

  protected implicit def resultAsMultiBulk(future: Future[RedisType]): Future[Option[List[Option[ByteString]]]] = future map toMultiBulk
  protected implicit def resultAsMultiBulkList(future: Future[RedisType]): Future[List[Option[ByteString]]] = future map toMultiBulkList
  protected implicit def resultAsMultiBulkFlat(future: Future[RedisType]): Future[Option[List[ByteString]]] = future map toMultiBulkFlat
  protected implicit def resultAsMultiBulkSet(future: Future[RedisType]): Future[Set[ByteString]] = future map toMultiBulkSet
  protected implicit def resultAsMultiBulkSinglePair(future: Future[RedisType]): Future[Option[(ByteString, ByteString)]] = future map toMultiBulkSinglePair
  protected implicit def resultAsMultiBulkSinglePairK[K: Parse](future: Future[RedisType]): Future[Option[(K, ByteString)]] = future map (toMultiBulkSinglePair(_).map(kv => (Parse(kv._1), kv._2)))
  protected implicit def resultAsBulk(future: Future[RedisType]): Future[Option[ByteString]] = future map toBulk
  protected implicit def resultAsLong(future: Future[RedisType]): Future[Long] = future map toLong
  protected implicit def resultAsInt(future: Future[RedisType]): Future[Int] = future map (toLong(_).toInt)
  protected implicit def resultAsIntOption(future: Future[RedisType]): Future[Option[Int]] = future map (x => Some(toLong(x).toInt).filter(_ > -1))
  protected implicit def resultAsBool(future: Future[RedisType]): Future[Boolean] = future map toBool
  protected implicit def resultAsStatus(future: Future[RedisType]): Future[String] = future map toStatus
  protected implicit def resultAsOkStatus(future: Future[RedisType]): Future[Unit] = future map toOkStatus

}

trait RedisClientSync extends Commands {
  type RawResult = RedisType
  type Result[A] = A

  protected def send(in: List[ByteString]): RedisType = (actor !!! Request(format(in))).get

  protected implicit def resultAsMultiBulk(raw: RedisType): Option[List[Option[ByteString]]] = toMultiBulk(raw)
  protected implicit def resultAsMultiBulkList(raw: RedisType): List[Option[ByteString]] = toMultiBulkList(raw)
  protected implicit def resultAsMultiBulkFlat(raw: RedisType): Option[List[ByteString]] = toMultiBulkFlat(raw)
  protected implicit def resultAsMultiBulkSet(raw: RedisType): Set[ByteString] = toMultiBulkSet(raw)
  protected implicit def resultAsMultiBulkSinglePair(raw: RedisType): Option[(ByteString, ByteString)] = toMultiBulkSinglePair(raw)
  protected implicit def resultAsMultiBulkSinglePairK[K: Parse](raw: RedisType): Option[(K, ByteString)] = toMultiBulkSinglePair(raw).map(kv => (Parse(kv._1), kv._2))
  protected implicit def resultAsBulk(raw: RedisType): Option[ByteString] = toBulk(raw)
  protected implicit def resultAsLong(raw: RedisType): Long = toLong(raw)
  protected implicit def resultAsInt(raw: RedisType): Int = toLong(raw).toInt
  protected implicit def resultAsIntOption(raw: RedisType): Option[Int] = Some(toLong(raw).toInt).filter(_ > -1)
  protected implicit def resultAsBool(raw: RedisType): Boolean = toBool(raw)
  protected implicit def resultAsStatus(raw: RedisType): String = toStatus(raw)
  protected implicit def resultAsOkStatus(raw: RedisType): Unit = toOkStatus(raw)

}

trait RedisClientQuiet extends Commands {
  type RawResult = Unit
  type Result[_] = Unit

  protected def send(in: List[ByteString]) = actor ! Request(format(in))

  protected implicit def resultAsMultiBulk(raw: Unit): Unit = ()
  protected implicit def resultAsMultiBulkList(raw: Unit): Unit = ()
  protected implicit def resultAsMultiBulkFlat(raw: Unit): Unit = ()
  protected implicit def resultAsMultiBulkSet(raw: Unit): Unit = ()
  protected implicit def resultAsMultiBulkSinglePair(raw: Unit): Unit = ()
  protected implicit def resultAsMultiBulkSinglePairK[K: Parse](raw: Unit): Unit = ()
  protected implicit def resultAsBulk(raw: Unit): Unit = ()
  protected implicit def resultAsLong(raw: Unit): Unit = ()
  protected implicit def resultAsInt(raw: Unit): Unit = ()
  protected implicit def resultAsIntOption(raw: Unit): Unit = ()
  protected implicit def resultAsBool(raw: Unit): Unit = ()
  protected implicit def resultAsStatus(raw: Unit): Unit = ()
  protected implicit def resultAsOkStatus(raw: Unit): Unit = ()
}

import commands._
trait Commands extends StringCommands with GenericCommands with ListCommands{
  type RawResult
  type Result[_]

  val actor: ActorRef

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
  protected implicit def resultAsMultiBulkSet(raw: RawResult): Result[Set[ByteString]]
  protected implicit def resultAsMultiBulkSinglePair(raw: RawResult): Result[Option[(ByteString, ByteString)]]
  protected implicit def resultAsMultiBulkSinglePairK[K: Parse](raw: RawResult): Result[Option[(K, ByteString)]]
  protected implicit def resultAsBulk(raw: RawResult): Result[Option[ByteString]]
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

  protected val toMultiBulkSet: RedisType => Set[ByteString] = _ match {
      case RedisMulti(Some(m)) => m.flatMap(toBulk(_).toList)(collection.breakOut)
      case RedisMulti(None) => Set.empty
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

  protected val toLong: RedisType => Long = _ match {
      case RedisInteger(n) => n
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

trait SortOrder
object SortOrder {
  case object ASC extends SortOrder
  case object DESC extends SortOrder
}

trait AggregateScore {
  def getBytes: Array[Byte]
}
object AggregateScore {
  case object SUM extends AggregateScore {
    val getBytes = "SUM".getBytes
  }
  case object MIN extends AggregateScore {
    val getBytes = "MIN".getBytes
  }
  case object MAX extends AggregateScore {
    val getBytes = "MAX".getBytes
  }
}

