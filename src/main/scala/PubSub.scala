package com.redis
/*
object Util {
  object Break extends RuntimeException;
  def break { throw Break }
  def whileTrue(block: => Unit) {
    try {
      while (true)
        try {
          block
        } catch { case Break => return }
    }
  }
}

sealed trait PubSubMessage
case class S(channel: Array[Byte], noSubscribed: Int) extends PubSubMessage
case class U(channel: Array[Byte], noSubscribed: Int) extends PubSubMessage
case class M(origChannel: Array[Byte], message: Array[Byte]) extends PubSubMessage

import Util._
trait PubSub { self: Redis =>
  var pubSub: Boolean = false

  class Consumer(fn: PubSubMessage => Any) extends Runnable {

    def start () {
      val myThread = new Thread(this) ;
      myThread.start() ;
    }

    def run {
      whileTrue {
        asList match {
          case Some(Some(msgType) :: Some(channel) :: Some(data) :: Nil) =>
            new String(msgType) match {
              case "subscribe" => fn(S(channel, new String(data).toInt))
              case "unsubscribe" if (new String(data).toInt == 0) => 
                println("for break")
                fn(U(channel, new String(data).toInt))
                break
              case "unsubscribe" => 
                fn(U(channel, new String(data).toInt))
              case "message" => 
                fn(M(channel, data))
              case x => throw new RuntimeException("unhandled message: " + x)
            }
          case _ => break
        }
      }
    }
  }

  def subscribe(channel: Array[Byte], channels: Array[Byte]*)(fn: PubSubMessage => Any) {
    if (pubSub == true) { // already pubsub ing
      subscribeRaw(channel, channels: _*)
      return
    }
    pubSub = true
    subscribeRaw(channel, channels: _*)
    new Consumer(fn).start
  }

  def subscribeRaw(channel: Array[Byte], channels: Array[Byte]*) {
    send("SUBSCRIBE", channel, channels: _*)
  }

  def unsubscribe = {
    send("UNSUBSCRIBE")
  }

  def unsubscribe(channel: Array[Byte], channels: Array[Byte]*) = {
    send("UNSUBSCRIBE", channel, channels: _*)
  }

  def publish(channel: Array[Byte], msg: Array[Byte]) = {
    send("PUBLISH", channel, msg)
  }
}
*/
