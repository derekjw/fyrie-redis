package net.fyrie
package redis

import actors._
import messages.{Request}
import Protocol.EOL
import types._

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
  protected implicit def resultAsBulk(future: Future[RedisType]): Future[Option[ByteString]] = future map toBulk
  protected implicit def resultAsLong(future: Future[RedisType]): Future[Long] = future map toLong
  protected implicit def resultAsBool(future: Future[RedisType]): Future[Boolean] = future map toBool
  protected implicit def resultAsOkStatus(future: Future[RedisType]): Future[Unit] = future map toOkStatus

}

trait RedisClientSync extends Commands {
  type RawResult = RedisType
  type Result[A] = A

  protected def send(in: List[ByteString]): RedisType = (actor !!! Request(format(in))).get

  protected implicit def resultAsMultiBulk(raw: RedisType): Option[List[Option[ByteString]]] = toMultiBulk(raw)
  protected implicit def resultAsBulk(raw: RedisType): Option[ByteString] = toBulk(raw)
  protected implicit def resultAsLong(raw: RedisType): Long = toLong(raw)
  protected implicit def resultAsBool(raw: RedisType): Boolean = toBool(raw)
  protected implicit def resultAsOkStatus(raw: RedisType): Unit = toOkStatus(raw)

}

trait RedisClientQuiet extends Commands {
  type RawResult = Unit
  type Result[_] = Unit

  protected def send(in: List[ByteString]) = actor ! Request(format(in))

  protected implicit def resultAsMultiBulk(raw: Unit): Unit = ()
  protected implicit def resultAsBulk(raw: Unit): Unit = ()
  protected implicit def resultAsLong(raw: Unit): Unit = ()
  protected implicit def resultAsBool(raw: Unit): Unit = ()
  protected implicit def resultAsOkStatus(raw: Unit): Unit = ()
}

trait Commands extends commands.StringCommands with commands.GenericCommands {
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
  protected implicit def resultAsBulk(raw: RawResult): Result[Option[ByteString]]
  protected implicit def resultAsLong(raw: RawResult): Result[Long]
  protected implicit def resultAsBool(raw: RawResult): Result[Boolean]
  protected implicit def resultAsOkStatus(raw: RawResult): Result[Unit]

  protected val toMultiBulk: RedisType => Option[List[Option[ByteString]]] = _ match {
      case RedisMulti(m) => m map (_ map toBulk)
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

