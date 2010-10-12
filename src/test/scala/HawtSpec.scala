package net.fyrie.redis
package akka

import Commands._
import actors._
import messages._

import org.specs._

import se.scalablesolutions.akka.actor.{Actor, ActorRef}
import Actor.{actorOf}

class RedisHawtSpec extends Specification {
  "test client" should {
    "handle simple request" in {
      val r = actorOf(new RedisClientSession("localhost", 16379)).start
      r ! Request(dbsize.toBytes, dbsize.handler)
      r ! Request(select(0).toBytes, dbsize.handler)
      r ! Request(dbsize.toBytes, dbsize.handler)
      r ! Request(randomkey().toBytes, randomkey().handler)
      r ! Request(randomkey().toBytes, randomkey().handler)
      r ! Request(dbsize.toBytes, dbsize.handler)
      r ! Request(randomkey().toBytes, randomkey().handler)
      r ! Request(randomkey().toBytes, randomkey().handler)
      r ! Request(keys().toBytes, randomkey().handler)
      r ! Request(dbsize.toBytes, dbsize.handler)
      Thread.sleep(5000)
    }
  }
}
