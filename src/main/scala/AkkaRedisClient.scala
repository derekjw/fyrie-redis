package net.fyrie
package redis
package akka

import actors.{RedisActor}
import messages.{Request}

import com.redis.commands.{Command}

import se.scalablesolutions.akka.dispatch.{Future}
import se.scalablesolutions.akka.actor.{Actor,ActorRef}
import Actor.{actorOf}

class AkkaRedisClient(address: String = "localhost", port: Int = 6379) {
  val actorRef = actorOf(new RedisActor(address, port)).start

  def !(command: Command[_])(implicit sender: Option[ActorRef] = None): Unit = actorRef ! Request(command, sender.isDefined)
  def !![T](command: Command[T]): Option[T] = (this !!! command).await.result
  def !!![T](command: Command[T]): Future[T] = actorRef !!! Request(command, true)

  def stop = actorRef.stop
}
