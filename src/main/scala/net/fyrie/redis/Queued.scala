package net.fyrie.redis

import types._

import akka.dispatch.{ Future, CompletableFuture ⇒ Promise }
import akka.util.ByteString

object Queued {
  private[redis] def apply[A](value: A, request: (ByteString, Promise[RedisType]), response: Promise[RedisType]): Queued[A] =
    new QueuedSingle(value, request, response)
  def apply[A](value: A): Queued[A] = new QueuedValue(value)

  private[redis] final class QueuedValue[+A](val value: A) extends Queued[A] {
    def requests = Vector.empty
    def responses = Vector.empty
    def flatMap[B](f: A ⇒ Queued[B]): Queued[B] = f(value)
    def map[B](f: A ⇒ B): Queued[B] = new QueuedValue(f(value))
  }

  private[redis] final class QueuedSingle[+A](val value: A, val request: (ByteString, Promise[RedisType]), val response: Promise[RedisType]) extends Queued[A] {
    def requests = Vector(request)
    def responses = Vector(response)
    def flatMap[B](f: A ⇒ Queued[B]): Queued[B] = {
      val that = f(value)
      new QueuedList(that.value, request +: that.requests, response +: that.responses)
    }
    def map[B](f: A ⇒ B): Queued[B] = new QueuedSingle(f(value), request, response)
  }

  private[redis] final class QueuedList[+A](val value: A, val requests: Vector[(ByteString, Promise[RedisType])], val responses: Vector[Promise[RedisType]]) extends Queued[A] {
    def flatMap[B](f: A ⇒ Queued[B]): Queued[B] = {
      f(value) match {
        case that: QueuedList[_] ⇒
          new QueuedList(that.value, requests ++ that.requests, responses ++ that.responses)
        case that: QueuedSingle[_] ⇒
          new QueuedList(that.value, requests :+ that.request, responses :+ that.response)
        case that: QueuedValue[_] ⇒
          new QueuedList(that.value, requests, responses)
      }
    }
    def map[B](f: A ⇒ B): Queued[B] = new QueuedList(f(value), requests, responses)
  }
}

sealed trait Queued[+A] {
  def value: A
  private[redis] def requests: Vector[(ByteString, Promise[RedisType])]
  private[redis] def responses: Vector[Promise[RedisType]]
  def flatMap[B](f: A ⇒ Queued[B]): Queued[B]
  def map[B](f: A ⇒ B): Queued[B]
}
