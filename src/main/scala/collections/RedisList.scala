package net.fyrie.redis
package akka
package collection

import serialization._

import scala.collection.mutable.{IndexedSeq}

import se.scalablesolutions.akka.dispatch.{Future}

object RedisList {
  def apply[A](name: String)(implicit conn: AkkaRedisClient, format: Format, parser: Parse[A]): RedisList[A] =
    new RedisList[A](name)(conn, format, parser)
}

class RedisList[A](name: String)(implicit conn: AkkaRedisClient, format: Format, parser: Parse[A]) extends IndexedSeq[A] {
  protected val key = name.getBytes

  def update(idx: Int, elem: A): Unit = conn ! Commands.lset(key, idx, elem)
  def apply(idx: Int): A = conn send Commands.lindex(key, idx) getOrElse (throw new IndexOutOfBoundsException)
  def length: Int = conn send Commands.llen(key)

  def +=(elem: A): RedisList[A] = {
    rpush(elem)
    this
  }

  def +=(elem1: A, elem2: A, elems: A*): RedisList[A] = {
    conn ! Commands.multiexec(
      Commands.rpush(key, elem1) ::
      Commands.rpush(key, elem2) ::
      elems.map(e => Commands.rpush(key, e)).toList)
    this
  }

  def +=:(elem: A): RedisList[A] = {
    lpush(elem)
    this
  }

  override def slice(from: Int, until: Int): IndexedSeq[A] =
    IndexedSeq((conn send Commands.lrange(key, from, until)).toList.flatten:_*)

  def lpush(elem: A) { conn ! Commands.lpush(key, elem) }    

  def rpush(elem: A) { conn ! Commands.rpush(key, elem) }

  def lpop: A = conn send Commands.lpop(key) getOrElse (throw new NoSuchElementException)

  def rpop: A = conn send Commands.rpop(key) getOrElse (throw new NoSuchElementException)

  def lpopFuture: Future[Result[A]] = conn !!! Commands.lpop(key)

  def rpopFuture: Future[Result[A]] = conn !!! Commands.rpop(key)

  override def toString: String = "RedisList("+name+")"

}
