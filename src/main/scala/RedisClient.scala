package net.fyrie
package redis

import actors._
import messages.{Request}

import se.scalablesolutions.akka.dispatch.{Future, FutureTimeoutException}
import se.scalablesolutions.akka.actor.{Actor,ActorRef}
import Actor.{actorOf}
import se.scalablesolutions.akka.dispatch._
import se.scalablesolutions.akka.config.Config._

import org.fusesource.hawtdispatch.ScalaDispatch._

class RedisClient(host: String = config.getString("fyrie-redis.host", "localhost"),
                  port: Int = config.getInt("fyrie-redis.port", 6379)) {
  val actor = actorOf(new RedisClientSession(host, port)).start

  def !(command: Command[_,_])(implicit sender: Option[ActorRef] = None): Unit =
    actor ! Request(command.toBytes, command.handler)

  def !![A,B](command: Command[A,B]): Option[B] = {
    val future = this !!! command
    try {
      future.await
    } catch {
      case e: FutureTimeoutException => None
    }
    if (future.exception.isDefined) throw future.exception.get
    else future.result
  }

  def !!![A,B](command: Command[A,B]): Future[B] =
    (actor !!! Request(command.toBytes, command.handler)).map(command.handler.parseResult)

  def send[A,B](command: Command[A,B]): B = {
    val future = this !!! command
    future.await
    if (future.exception.isDefined) throw future.exception.get
    else future.result.get
  }

  def stop = {
    actor.stop
  }

  def disconnect = stop

  def shutdown = {
    Dispatchers.globalHawtDispatcher.shutdown
    val gl = (globalQueue.asInstanceOf[org.fusesource.hawtdispatch.internal.GlobalDispatchQueue])
    gl.shutdown()
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
