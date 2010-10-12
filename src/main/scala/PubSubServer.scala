package com.redis
/*
import scala.actors._
import scala.actors.Actor._

sealed trait Msg
case class Subscribe(channels: Array[Array[Byte]]) extends Msg
case class Register(callback: PubSubMessage => Any) extends Msg
case class Unsubscribe(channels: Array[Array[Byte]]) extends Msg
case object UnsubscribeAll extends Msg
case class Publish(channel: Array[Byte], msg: Array[Byte]) extends Msg

class Subscriber(client: RedisClient) extends Actor {
  var callback: PubSubMessage => Any = { m => }

  def act = {
    loop {
      react {
        case Subscribe(channels) =>
          client.subscribe(channels.head, channels.tail: _*)(callback)
          reply(true)

        case Register(cb) =>
          callback = cb
          reply(true)

        case Unsubscribe(channels) =>
          client.unsubscribe(channels.head, channels.tail: _*)
          reply(true)

        case UnsubscribeAll =>
          client.unsubscribe
          reply(true)
      }
    }
  }
}

class Publisher(client: RedisClient) extends Actor {
  def act = {
    loop {
      react {
        case Publish(channel, message) =>
          client.publish(channel, message)
          reply(true)
      }
    }
  }
}

*/
