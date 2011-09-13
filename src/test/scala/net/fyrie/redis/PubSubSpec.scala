package net.fyrie.redis

import org.specs2._
import execute._
import specification._

import akka.dispatch.Future
import akka.actor.{ Actor, ActorRef }
import Actor.actorOf
import pubsub._
import serialization.Parse

import java.util.concurrent.LinkedBlockingQueue

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
    val redis = RedisClient()
    val q = new LinkedBlockingQueue[String]()
    val sub = actorOf(new TestSubscriber(redis, q)).start

    def get(msgs: String*) = ((success: Result) /: msgs)((r, m) ⇒ r and (q.take must_== m))

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

class TestSubscriber(redisClient: RedisClient, q: LinkedBlockingQueue[String]) extends Actor {
  var client: ActorRef = _
  override def preStart {
    client = redisClient.subscriber(self)
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
