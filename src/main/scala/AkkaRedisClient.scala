package net.fyrie
package redis
package akka

import actors.{RedisPipelineActor, RedisWorkerActor}
import messages.{Request, Work}

import se.scalablesolutions.akka.dispatch.{Future, FutureTimeoutException}
import se.scalablesolutions.akka.actor.{Actor,ActorRef}
import Actor.{actorOf}
import se.scalablesolutions.akka.dispatch._

class AkkaRedisClient(address: String = "localhost", port: Int = 6379)(implicit dispatcher: MessageDispatcher = Dispatchers.defaultGlobalDispatcher) {
  val actorRef = actorOf(new RedisPipelineActor(address, port)).start

  def ![A](command: Command[A])(implicit sender: Option[ActorRef] = None): Unit =
    actorRef ! Request(command.toBytes, command.handler, sender.isDefined)

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
    actorRef !!! Request(command.toBytes, command.handler, true)

  def send[A](command: Command[A]): A = {
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

  def ![A](command: Command[A])(implicit sender: Option[ActorRef] = None): Unit =
    actorRef ! Work(command)

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
    actorRef !!! Work(command)

  def send[A](command: Command[A]): A = {
    val future = this !!! command
    future.await
    if (future.exception.isDefined) throw future.exception.get
    else future.result.get
  }

  def stop = pool.foreach(_.stop)

  def disconnect = stop

}
