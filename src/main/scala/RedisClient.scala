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

class RedisClient(host: String = "localhost", port: Int = 6379) extends commands.StringCommands with commands.GenericCommands {
  val ioManager = actorOf(new IOManager()).start
  val actor = actorOf(new RedisClientSession(ioManager, host, port)).start

  protected def send(in: List[ByteString]): Future[RedisType] = {
    var count = 0
    var cmd = ByteString.empty
    in foreach { bytes =>
      count += 1
      cmd ++= ByteString("$"+bytes.length) ++ EOL ++ bytes ++ EOL
    }
    actor !!! Request(ByteString("*"+count) ++ EOL ++ cmd)
  }

  protected implicit def mapMultiBulk(future: Future[RedisType]): Future[Option[List[Option[ByteString]]]] = future map {
      _ match {
        case RedisMulti(m) => m map {
          _ map {
            _ match {
              case RedisBulk(b) => b
              case RedisError(e) => throw RedisErrorException(e)
              case _ => throw RedisProtocolException("Unexpected response")
            }
          }
        }
        case RedisError(e) => throw RedisErrorException(e)
        case _ => throw RedisProtocolException("Unexpected response")
      }
    }

  protected implicit def mapBulk(future: Future[RedisType]): Future[Option[ByteString]] = future map {
      _ match {
        case RedisBulk(b) => b
        case RedisError(e) => throw RedisErrorException(e)
        case _ => throw RedisProtocolException("Unexpected response")
      }
    }

  protected implicit def mapLong(future: Future[RedisType]): Future[Long] = future map {
      _ match {
        case RedisInteger(n) => n
        case RedisError(e) => throw RedisErrorException(e)
        case _ => throw RedisProtocolException("Unexpected response")
      }
    }

  protected implicit def mapBool(future: Future[RedisType]): Future[Boolean] = future map {
      _ match {
        case RedisInteger(1) => true
        case RedisInteger(0) => false
        case RedisError(e) => throw RedisErrorException(e)
        case _ => throw RedisProtocolException("Unexpected response")
      }
    }

  protected implicit def mapOkStatus(future: Future[RedisType]): Future[Unit] = future map {
      _ match {
        case RedisString("OK") => Unit
        case RedisError(e) => throw RedisErrorException(e)
        case _ => throw RedisProtocolException("Unexpected response")
      }
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

