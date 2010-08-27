package net.fyrie.redis
package akka
package collection

import scala.collection.mutable.{IndexedSeq}

import se.scalablesolutions.akka.dispatch.{Future}

object RedisList {
  def apply[A](name: String)(implicit conn: AkkaRedisClient, toBytes: (A) => Array[Byte], fromBytes: (Array[Byte]) => A): RedisList[A] =
    new RedisList[A](name)(conn, toBytes, fromBytes)
}

class RedisList[A](name: String)(implicit conn: AkkaRedisClient, toBytes: (A) => Array[Byte], fromBytes: (Array[Byte]) => A) extends IndexedSeq[A] {
  protected val key = name.getBytes

  protected implicit def fromBulk(in: Option[Array[Byte]]): Option[A] = in.map(fromBytes)

  def update(idx: Int, elem: A): Unit = conn ! commands.lset(key, idx, elem)
  def apply(idx: Int): A = conn send commands.lindex(key, idx) getOrElse (throw new IndexOutOfBoundsException)
  def length: Int = conn send commands.llen(key)

  def +=(elem: A): RedisList[A] = {
    rpush(elem)
    this
  }

  def +=(elem1: A, elem2: A, elems: A*): RedisList[A] = {
    conn ! commands.multiexec(
      commands.rpush(key, elem1) ::
      commands.rpush(key, elem2) ::
      elems.map(e => commands.rpush(key, e)).toList)
    this
  }

  def +=:(elem: A): RedisList[A] = {
    lpush(elem)
    this
  }

  override def slice(from: Int, until: Int): IndexedSeq[A] =
    (conn send commands.lrange(key, from, until))(x => IndexedSeq[A](x.toList.flatten.map(fromBytes): _*))

  def lpush(elem: A) { conn ! commands.lpush(key, elem) }    

  def rpush(elem: A) { conn ! commands.rpush(key, elem) }

  def lpop: A = conn send commands.lpop(key) getOrElse (throw new NoSuchElementException)

  def rpop: A = conn send commands.rpop(key) getOrElse (throw new NoSuchElementException)

  def lpopFuture: Future[Option[A]] = conn !!! commands.lpop(key)

  def rpopFuture: Future[Option[A]] = conn !!! commands.rpop(key)

  override def toString: String = "RedisList("+name+")"

}
