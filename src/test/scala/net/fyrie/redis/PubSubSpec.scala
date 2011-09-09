package net.fyrie.redis

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers

import akka.dispatch.{ Future, PromiseStream }
import akka.testkit.{ filterEvents, EventFilter }
import akka.actor.{ Actor, ActorRef }
import Actor.actorOf
import pubsub._
import serialization.Parse

class PubSubSpec extends Spec
  with ShouldMatchers
  with RedisTestServer {

  class TestSubscriber(ps: PromiseStream[String]) extends Actor {
    var client: ActorRef = _
    override def preStart {
      client = r.subscriber(self)
    }

    def receive = {
      case msg: Subscribe                    ⇒ client ! msg
      case msg: Unsubscribe                  ⇒ client ! msg
      case msg: PSubscribe                   ⇒ client ! msg
      case msg: PUnsubscribe                 ⇒ client ! msg
      case Subscribed(channel, count)        ⇒ ps enqueue ("[" + Parse[String](channel) + "] Subscribed")
      case Unsubscribed(channel, count)      ⇒ ps enqueue ("[" + Parse[String](channel) + "] Unsubscribed")
      case Message(channel, bytes)           ⇒ ps enqueue ("[" + Parse[String](channel) + "] Message: " + Parse[String](bytes))
      case PSubscribed(pattern, count)       ⇒ ps enqueue ("[" + Parse[String](pattern) + "] Subscribed")
      case PUnsubscribed(pattern, count)     ⇒ ps enqueue ("[" + Parse[String](pattern) + "] Unsubscribed")
      case PMessage(pattern, channel, bytes) ⇒ ps enqueue ("[" + Parse[String](pattern) + "][" + Parse[String](channel) + "] Message: " + Parse[String](bytes))
    }
  }

  describe("pubsub") {
    it("should work") {
      val ps = PromiseStream[String]()
      val s = actorOf(new TestSubscriber(ps))
      s ! Subscribe(Seq("Test Channel", "Test Channel 2"))
      ps.dequeue.get should be("[Test Channel] Subscribed")
      ps.dequeue.get should be("[Test Channel 2] Subscribed")
      r.publish("Test Channel", "Hello!")
      r.publish("Test Channel", "World!")
      ps.dequeue.get should be("[Test Channel] Message: Hello!")
      ps.dequeue.get should be("[Test Channel] Message: World!")
      s.stop
    }
  }
}
