/* package net.fyrie.redis

import org.specs2._
import execute._
import specification._

import akka.dispatch.{ Future, PromiseStream }
import akka.testkit.{ filterEvents, EventFilter }
import akka.actor.{ Actor, ActorRef }
import pubsub._
import serialization.Parse

class PubSubSpec extends Specification with PubSubHelper {

  def is = args(sequential = true) ^
    p ^
    "A PubSub Subscriber" ^
    "subscribing to channels and a pattern" ! {
      pubsub.subscribe("Test Channel", "Test Channel 2") and pubsub.psubscribe("Test*")
    } ^
    "should be notified of subscription" ! {
      pubsub.get("[Test Channel] Subscribed", "[Test Channel 2] Subscribed", "[Test*] Subscribed")
    } ^
    "and when messages are published" ! {
      pubsub.publish("Test Channel", "Hello!") and pubsub.publish("Test Channel 3", "World!")
    } ^
    "they should be received by the subscriber." ! {
      pubsub.get("[Test Channel] Message: Hello!", "[Test*][Test Channel] Message: Hello!", "[Test*][Test Channel 3] Message: World!")
    } ^
    pubsub.stop ^
    end

}

trait PubSubHelper { self: Specification ⇒
  val pubsub = new {
    implicit val system = TestSystem.system
    val redis = RedisClient()
    val ps = PromiseStream[String]()
    val sub = system.actorOf(new TestSubscriber(redis, ps))

    def get(msgs: String*) = ((success: Result) /: msgs)((r, m) ⇒ r and (ps.dequeue.get must_== m))

    def subscribe(channels: String*) = {
      sub ! Subscribe(channels)
      success
    }

    def psubscribe(patterns: String*) = {
      sub ! PSubscribe(patterns)
      success
    }

    def publish(channel: String, msg: String) = {
      redis.publish(channel, msg)
      success
    }

    def stop = Step {
      sub.stop
      redis.disconnect
    }

  }
}

class TestSubscriber(redisClient: RedisClient, ps: PromiseStream[String]) extends Actor {
  var client: ActorRef = _
  override def preStart {
    client = redisClient.subscriber(self)
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

*/
