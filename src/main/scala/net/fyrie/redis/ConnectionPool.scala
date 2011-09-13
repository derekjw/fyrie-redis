package net.fyrie
package redis

import messages.{ RequestClient, ReleaseClient, Disconnect }
import akka.actor._
import akka.actor.Actor.actorOf
import akka.dispatch.{ CompletableFuture ⇒ Promise, Future }
import collection.immutable.Queue

private[redis] class ConnectionPool(initialSize: Range, factory: () ⇒ RedisClientPoolWorker) extends Actor {
  var ready: List[RedisClientPoolWorker] = Nil
  var active: Set[RedisClientPoolWorker] = Set.empty
  var limit: Range = initialSize
  var size: Int = 0
  var queue: Queue[Promise[RedisClientPoolWorker]] = Queue.empty

  def receive = {
    case RequestClient(promise) ⇒
      if (active.size >= limit.max)
        queue = queue enqueue promise
      else {
        val client = ready match {
          case h :: t ⇒
            ready = t
            h
          case _ ⇒
            size += 1
            factory()
        }
        active += client
        promise complete Right(client)
      }
    case ReleaseClient(client) ⇒
      if (active.size > limit.max) {
        killClient(client)
      } else if (queue.nonEmpty) {
        val (promise, rest) = queue.dequeue
        promise complete Right(client)
        queue = rest
      } else if ((size - active.size) == limit.min) {
        killClient(client)
      } else {
        ready ::= client
        active -= client
      }
    case Disconnect ⇒
      ready foreach (_.disconnect)
      ready = Nil
      active foreach (_.disconnect)
      active = Set.empty
      queue foreach (_ complete Left(RedisConnectionException("Connection pool shutting down")))
      self.stop

  }

  def killClient(client: RedisClientPoolWorker) {
    client.disconnect
    active -= client
    size -= 1
  }

}
