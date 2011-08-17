package net.fyrie.redis
package pubsub

import akka.util.ByteString
import serialization.Store

sealed trait PubSubMessage
case class Subscribed(channel: ByteString, count: Long) extends PubSubMessage
case class Unsubscribed(channel: ByteString, count: Long) extends PubSubMessage
case class Message(channel: ByteString, content: ByteString) extends PubSubMessage
case class PSubscribed(pattern: ByteString, count: Long) extends PubSubMessage
case class PUnsubscribed(pattern: ByteString, count: Long) extends PubSubMessage
case class PMessage(pattern: ByteString, channel: ByteString, content: ByteString) extends PubSubMessage

object Subscribe {
  def apply[A: Store](channel: A): Subscribe = new Subscribe(List(Store(channel)))
  def apply[A: Store](channels: Iterable[A]): Subscribe = new Subscribe(channels.map(Store(_))(collection.breakOut))
}
case class Subscribe(channels: List[ByteString]) extends PubSubMessage

object Unsubscribe {
  def apply[A: Store](channel: A): Unsubscribe = new Unsubscribe(List(Store(channel)))
  def apply[A: Store](channels: Iterable[A]): Unsubscribe = new Unsubscribe(channels.map(Store(_))(collection.breakOut))
}
case class Unsubscribe(channels: List[ByteString]) extends PubSubMessage

object PSubscribe {
  def apply[A: Store](pattern: A): PSubscribe = new PSubscribe(List(Store(pattern)))
  def apply[A: Store](patterns: Iterable[A]): PSubscribe = new PSubscribe(patterns.map(Store(_))(collection.breakOut))
}
case class PSubscribe(patterns: List[ByteString]) extends PubSubMessage

object PUnsubscribe {
  def apply[A: Store](pattern: A): PUnsubscribe = new PUnsubscribe(List(Store(pattern)))
  def apply[A: Store](patterns: Iterable[A]): PUnsubscribe = new PUnsubscribe(patterns.map(Store(_))(collection.breakOut))
}
case class PUnsubscribe(patterns: List[ByteString]) extends PubSubMessage
