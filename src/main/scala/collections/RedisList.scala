package net.fyrie.redis.akka
package collection

import com.redis._

import scala.collection.mutable.{IndexedSeq}

object RedisList {
  def apply[A](name: String)(implicit conn: AkkaRedisClient, toBytes: (A) => Array[Byte], fromBytes: (Array[Byte]) => A): RedisList[A] =
    new RedisList[A](name, conn)(toBytes, fromBytes)
}

class RedisList[A](name: String, conn: AkkaRedisClient)(implicit toBytes: (A) => Array[Byte], fromBytes: (Array[Byte]) => A) extends IndexedSeq[A] {
  protected val nameBytes = name.getBytes
  def update(idx: Int, elem: A): Unit = conn ! commands.lset(nameBytes, idx, toBytes(elem))
  def apply(idx: Int): A = conn !! commands.lindex(nameBytes, idx) getOrElse error("Timed out") map (fromBytes) getOrElse (throw new IndexOutOfBoundsException)
  def length: Int = conn !! commands.llen(nameBytes) getOrElse error("Timed out")

  def +=(elem: A): RedisList[A] = {
    rpush(toBytes(elem))
    this
  }

  def +=(elem1: A, elem2: A, elems: A*): RedisList[A] = {
    conn ! commands.multiexec(
      commands.rpush(nameBytes, toBytes(elem1)) ::
      commands.rpush(nameBytes, toBytes(elem2)) ::
      elems.map(e => commands.rpush(nameBytes, toBytes(e))).toList)
    this
  }

  def +=:(elem: A): RedisList[A] = {
    lpush(toBytes(elem))
    this
  }

  override def slice(from: Int, until: Int): IndexedSeq[A] =
    IndexedSeq[A]((conn !! commands.lrange(nameBytes, from, until) getOrElse error("Timed out")).toList.flatten.map(fromBytes) : _*)

  def lpush(elem: A) { conn ! commands.lpush(nameBytes, elem) }    

  def rpush(elem: A) { conn ! commands.rpush(nameBytes, elem) }

  def lpop: A = fromBytes(conn !! commands.lpop(nameBytes) getOrElse error("Timed out") getOrElse (throw new NoSuchElementException))

  def rpop: A = fromBytes(conn !! commands.rpop(nameBytes) getOrElse error("Timed out") getOrElse (throw new NoSuchElementException))

  override def toString: String = "RedisList of "+length+" items"

}
