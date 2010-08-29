package net.fyrie.redis
package akka
package collection

import se.scalablesolutions.akka.dispatch.{Future}

import scala.collection.mutable

object RedisSet {
  def apply[A](name: String)(implicit conn: AkkaRedisClient, toBytes: (A) => Array[Byte], fromBytes: (Array[Byte]) => A): RedisSet[A] =
    new RedisSet[A](name)(conn, toBytes, fromBytes)
}

class RedisSet[A](name: String)(implicit conn: AkkaRedisClient, toBytes: (A) => Array[Byte], fromBytes: (Array[Byte]) => A) extends mutable.Set[A] {
  protected val rediskey = name.getBytes

  protected implicit def fromBulk(in: Option[Array[Byte]]): Option[A] = in.map(fromBytes)

  protected implicit def fromMultiBulk(in: Option[Set[Array[Byte]]]): Option[Set[A]] = in.map(_.map(fromBytes))

  override def repr: mutable.Set[A] =
    smembers.map(new mutable.ImmutableSetAdaptor(_)).getOrElse(empty)

  override def empty: mutable.Set[A] = mutable.Set.empty[A]

  def contains(key: A): Boolean = sismember(key)

  def iterator: Iterator[A] = repr.iterator

  override def size: Int = scard.toInt

  override def +=(elem: A): RedisSet.this.type = {saddFast(elem); this}

  override def +=(elem1: A, elem2: A, elems: A*): RedisSet.this.type = {
    conn ! commands.multiexec(saddCmd(elem1) :: saddCmd(elem2) :: elems.map(saddCmd).toList)
    this
  }

  override def -=(elem: A): RedisSet.this.type = {sremFast(elem); this}

  override def -=(elem1: A, elem2: A, elems: A*): RedisSet.this.type = {
    conn ! commands.multiexec(sremCmd(elem1) :: sremCmd(elem2) :: elems.map(sremCmd).toList)
    this
  }

  def saddCmd(elem: A) = commands.sadd(rediskey, elem)
  def sadd(elem: A): Boolean = conn send saddCmd(elem)
  def saddFast(elem: A): Unit = conn ! saddCmd(elem)
  def saddFuture(elem: A): Future[Boolean] = conn !!! saddCmd(elem)

  def sremCmd(elem: A) = commands.srem(rediskey, elem)
  def srem(elem: A): Boolean = conn send sremCmd(elem)
  def sremFast(elem: A): Unit = conn ! sremCmd(elem)
  def sremFuture(elem: A): Future[Boolean] = conn !!! sremCmd(elem)

  def spopCmd = commands.spop(rediskey)
  def spop: Option[A] = conn send spopCmd
  def spopFast: Unit = conn ! spopCmd
  def spopFuture: Future[Option[A]] = conn !!! spopCmd

  def smoveCmd(dest: String, elem: A) = commands.smove(rediskey, dest.getBytes, elem)
  def smove(dest: String, elem: A): Boolean = conn send smoveCmd(dest, elem)
  def smoveFast(dest: String, elem: A): Unit = conn ! smoveCmd(dest, elem)
  def smoveFuture(dest: String, elem: A): Future[Boolean] = conn !!! smoveCmd(dest, elem)

  def scardCmd = commands.scard(rediskey)
  def scard: Long = conn send scardCmd

  def sismemberCmd(elem: A) = commands.sismember(rediskey, elem)
  def sismember(elem: A): Boolean = conn send sismemberCmd(elem)

  def smembersCmd = commands.smembers(rediskey)
  def smembers: Option[Set[A]] = conn send smembersCmd

  override def toString: String = "RedisSet("+name+")"

}
