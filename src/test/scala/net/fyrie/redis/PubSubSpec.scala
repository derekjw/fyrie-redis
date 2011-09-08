package net.fyrie.redis

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers

import akka.dispatch.Future
import akka.actor.{ Actor, ActorRef }
import Actor.actorOf
import pubsub._
import serialization.Parse

import java.util.concurrent.LinkedBlockingQueue

class PubSubSpec extends Spec
  with ShouldMatchers
  with RedisTestServer {

  class TestSubscriber(q: LinkedBlockingQueue[String]) extends Actor {
    var client: ActorRef = _
    override def preStart {
      client = r.subscriber(self)
    }

    def receive = {
      case msg: Subscribe                    ⇒ client ! msg
      case msg: Unsubscribe                  ⇒ client ! msg
      case msg: PSubscribe                   ⇒ client ! msg
      case msg: PUnsubscribe                 ⇒ client ! msg
      case Subscribed(channel, count)        ⇒ q put ("[" + Parse[String](channel) + "] Subscribed")
      case Unsubscribed(channel, count)      ⇒ q put ("[" + Parse[String](channel) + "] Unsubscribed")
      case Message(channel, bytes)           ⇒ q put ("[" + Parse[String](channel) + "] Message: " + Parse[String](bytes))
      case PSubscribed(pattern, count)       ⇒ q put ("[" + Parse[String](pattern) + "] Subscribed")
      case PUnsubscribed(pattern, count)     ⇒ q put ("[" + Parse[String](pattern) + "] Unsubscribed")
      case PMessage(pattern, channel, bytes) ⇒ q put ("[" + Parse[String](pattern) + "][" + Parse[String](channel) + "] Message: " + Parse[String](bytes))
    }
  }

  describe("pubsub") {
    it("should work") {
      val q = new LinkedBlockingQueue[String]()
      val s = actorOf(new TestSubscriber(q)).start
      s ! Subscribe(Seq("Test Channel", "Test Channel 2"))
      q.take should be("[Test Channel] Subscribed")
      q.take should be("[Test Channel 2] Subscribed")
      r.publish("Test Channel", "Hello!")
      r.publish("Test Channel", "World!")
      q.take should be("[Test Channel] Message: Hello!")
      q.take should be("[Test Channel] Message: World!")
      s.stop
    }
  }
}
