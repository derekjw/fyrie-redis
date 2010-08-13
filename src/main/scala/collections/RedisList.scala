package net.fyrie.redis
package akka
package collection

import scala.collection.mutable.{IndexedSeq}

object RedisList {
  def apply[A](name: String)(implicit conn: AkkaRedisClient, toBytes: (A) => Array[Byte], fromBytes: (Array[Byte]) => A): RedisList[A] =
    new RedisList[A](name)(conn, toBytes, fromBytes)
}

class RedisList[A](name: String)(implicit conn: AkkaRedisClient, toBytes: (A) => Array[Byte], fromBytes: (Array[Byte]) => A) extends IndexedSeq[A] {
  protected val key = name.getBytes
  def update(idx: Int, elem: A): Unit = conn ! commands.lset(key, idx, toBytes(elem))
  def apply(idx: Int): A = conn !! commands.lindex(key, idx) getOrElse error("Timed out") map (fromBytes) getOrElse (throw new IndexOutOfBoundsException)
  def length: Int = conn !! commands.llen(key) getOrElse error("Timed out")

  def +=(elem: A): RedisList[A] = {
    rpush(toBytes(elem))
    this
  }

  def +=(elem1: A, elem2: A, elems: A*): RedisList[A] = {
    conn ! commands.multiexec(
      commands.rpush(key, toBytes(elem1)) ::
      commands.rpush(key, toBytes(elem2)) ::
      elems.map(e => commands.rpush(key, toBytes(e))).toList)
    this
  }

  def +=:(elem: A): RedisList[A] = {
    lpush(toBytes(elem))
    this
  }

  override def slice(from: Int, until: Int): IndexedSeq[A] =
    IndexedSeq[A]((conn !! commands.lrange(key, from, until) getOrElse error("Timed out")).toList.flatten.map(fromBytes) : _*)

  def lpush(elem: A) { conn ! commands.lpush(key, elem) }    

  def rpush(elem: A) { conn ! commands.rpush(key, elem) }

  def lpop: A = fromBytes(conn !! commands.lpop(key) getOrElse error("Timed out") getOrElse (throw new NoSuchElementException))

  def rpop: A = fromBytes(conn !! commands.rpop(key) getOrElse error("Timed out") getOrElse (throw new NoSuchElementException))

  override def toString: String = "RedisList("+name+")"

}
