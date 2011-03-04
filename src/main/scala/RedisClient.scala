package net.fyrie
package redis

import actors._
import messages.{Request}
import handlers._

import akka.dispatch.{Future, FutureTimeoutException}
import akka.actor.{Actor,ActorRef}
import Actor.{actorOf}
import akka.dispatch._
import akka.config.Config._

class RedisClient(host: String = config.getString("fyrie-redis.host", "localhost"),
                  port: Int = config.getInt("fyrie-redis.port", 6379)) {
  val actor = actorOf(new RedisClientSession(host, port)).start

  def ![A](command: Command[A])(implicit sender: Option[ActorRef] = None): Unit =
    actor ! Request(command.toBytes, command.handler)

  def !![A](command: Command[A]): Option[A] = {
    val future = this !!! command
    try {
      future.await
    } catch {
      case e: FutureTimeoutException => None
    }
    if (future.exception.isDefined) throw future.exception.get
    else future.result
  }

  def !!![A](command: Command[A]): Future[A] =
    command.handler match {
      case sh: SingleHandler[_,_] =>
        actor !!! Request(command.toBytes, command.handler)
      case mh: MultiHandler[_] =>
        val future: Future[Option[Stream[Future[Any]]]] = actor !!! Request(command.toBytes, command.handler)
        future.map(x => mh.parse(x.map(_.map(f => FutureResponder.futureToResponse(f)))))
    }

  def send[A](command: Command[A]): A = {
    val future = this !!! command
    future.await
    if (future.exception.isDefined) throw future.exception.get
    else future.result.get
  }

  def stop = {
    actor.stop
  }

  def disconnect = stop

  def shutdownWorkarounds = {
    org.fusesource.hawtdispatch.ScalaDispatch.globalQueue.asInstanceOf[org.fusesource.hawtdispatch.internal.GlobalDispatchQueue].shutdown

    val tf = classOf[java.lang.Thread].getDeclaredField("subclassAudits")
    tf.setAccessible(true)
    val cache = tf.get(null).asInstanceOf[java.util.Map[_,_]]
    cache.synchronized {cache.clear}

    val lf = classOf[java.util.logging.Level].getDeclaredField("known")
    lf.setAccessible(true)
    val known = lf.get(null).asInstanceOf[java.util.ArrayList[java.util.logging.Level]]
    known.synchronized {
      known.clear
      known.add(java.util.logging.Level.OFF)
      known.add(java.util.logging.Level.SEVERE)
      known.add(java.util.logging.Level.WARNING)
      known.add(java.util.logging.Level.INFO)
      known.add(java.util.logging.Level.CONFIG)
      known.add(java.util.logging.Level.FINE)
      known.add(java.util.logging.Level.FINER)
      known.add(java.util.logging.Level.FINEST)
      known.add(java.util.logging.Level.ALL)
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
