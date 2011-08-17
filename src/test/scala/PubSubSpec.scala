package net.fyrie.redis

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers

import akka.dispatch.Future
import akka.testkit.{ filterEvents, EventFilter }
import akka.actor.{ Actor, ActorRef }
import Actor.actorOf
import pubsub._
import serialization.Parse

class PubSubSpec extends Spec
  with ShouldMatchers
  with RedisTestServer {

  class TestSubscriber extends Actor {
    var client: ActorRef = _
    override def preStart {
      client = r.subscriber(self)
    }

    def receive = {
      case msg: Subscribe => client ! msg
      case msg: Unsubscribe => client ! msg
      case msg: PSubscribe => client ! msg
      case msg: PUnsubscribe => client ! msg
      case Subscribed(channel, count) => println("[" + Parse[String](channel) + "] Subscribed")
      case Unsubscribed(channel, count) => println("[" + Parse[String](channel) + "] Unsubscribed")
      case Message(channel, bytes) => println("[" + Parse[String](channel) + "] Message: " + Parse[String](bytes))
    }
  }

  describe("pubsub") {
    it("should work") {
      val s = actorOf(new TestSubscriber).start
      s ! Subscribe(Set("Test Channel", "Test Channel 2"))
      Thread.sleep(1000)
      r.publish("Test Channel", "Hello!")
      r.publish("Test Channel", "World!")
      Thread.sleep(1000)
      s.stop
    }
  }
}
