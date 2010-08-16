package net.fyrie
package redis
package akka

import actors.{RedisActor}
import messages.{Request}

import se.scalablesolutions.akka.dispatch.{Future, FutureTimeoutException}
import se.scalablesolutions.akka.actor.{Actor,ActorRef}
import Actor.{actorOf}
import se.scalablesolutions.akka.dispatch._

object AkkaRedisClient {
  lazy val dispatcher: MessageDispatcher = new HawtDispatcher(false)
}

class AkkaRedisClient(address: String = "localhost", port: Int = 6379)(implicit dispatcher: MessageDispatcher = AkkaRedisClient.dispatcher) {
  val actorRef = actorOf(new RedisActor(address, port)).start

  def !(command: Command[_])(implicit sender: Option[ActorRef] = None): Unit = actorRef ! Request(command, sender.isDefined)
  def !![T](command: Command[T]): Option[T] = {
    val future = this !!! command
    try {
      future.await
    } catch {
      case e: FutureTimeoutException => None
    }
    if (future.exception.isDefined) throw future.exception.get
    else future.result
  }
  def !!![T](command: Command[T]): Future[T] = actorRef !!! Request(command, true)

  def send[T](command: Command[T]): T = {
    val future = this !!! command
    future.await
    if (future.exception.isDefined) throw future.exception.get
    else future.result.get
  }

  def stop = actorRef.stop

  def disconnect = stop
}
