package net.fyrie
package redis
package akka

import actors.{RedisPipelineActor, RedisWorkerActor}
import messages.{Request, Work}

import se.scalablesolutions.akka.dispatch.{Future, FutureTimeoutException}
import se.scalablesolutions.akka.actor.{Actor,ActorRef}
import Actor.{actorOf}
import se.scalablesolutions.akka.dispatch._

object AkkaRedisClient {
  lazy val dispatcher: MessageDispatcher = new HawtDispatcher(false)
}

class AkkaRedisClient(address: String = "localhost", port: Int = 6379)(implicit dispatcher: MessageDispatcher = AkkaRedisClient.dispatcher) {
  val actorRef = actorOf(new RedisPipelineActor(address, port)).start

  def ![A, B](command: Command[A, B])(implicit sender: Option[ActorRef] = None): Unit =
    actorRef ! Request(command, sender.isDefined)

  def !![A, B](command: Command[A, B]): Option[B] = {
    val future = this !!! command
    try {
      future.await
    } catch {
      case e: FutureTimeoutException => None
    }
    if (future.exception.isDefined) throw future.exception.get
    else future.result
  }

  def !!![A, B](command: Command[A, B]): Future[B] =
    actorRef !!! Request(command, true)

  def send[A, B](command: Command[A, B]): B = {
    val future = this !!! command
    future.await
    if (future.exception.isDefined) throw future.exception.get
    else future.result.get
  }

  def stop = actorRef.stop

  def disconnect = stop
}

class AkkaRedisWorkerPool(address: String = "localhost", port: Int = 6379, size: Int = 4, name: String = "AkkaRedisWorkers") {
  implicit val dispatcher = Dispatchers.newExecutorBasedEventDrivenWorkStealingDispatcher(name, 0)
  val pool = (1 to size).map(i => actorOf(new RedisWorkerActor(address, port)).start).toList
  val actorRef = pool.head

  def ![A, B](command: Command[A, B])(implicit sender: Option[ActorRef] = None): Unit =
    actorRef ! Work(command)

  def !![A, B](command: Command[A, B]): Option[B] = {
    val future = this !!! command
    try {
      future.await
    } catch {
      case e: FutureTimeoutException => None
    }
    if (future.exception.isDefined) throw future.exception.get
    else future.result
  }

  def !!![A, B](command: Command[A, B]): Future[B] =
    actorRef !!! Work(command)

  def send[A, B](command: Command[A, B]): B = {
    val future = this !!! command
    future.await
    if (future.exception.isDefined) throw future.exception.get
    else future.result.get
  }

  def stop = pool.foreach(_.stop)

  def disconnect = stop

}
