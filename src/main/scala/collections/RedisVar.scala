package net.fyrie.redis
package akka
package collection

object RedisVar {
  def apply[A](name: String, default: Option[A] = None)(implicit conn: AkkaRedisClient, toBytes: (A) => Array[Byte], fromBytes: (Array[Byte]) => A): RedisVar[A] =
    new RedisVar[A](name, default)(conn, toBytes, fromBytes)
}

/**  Modeled to be similar to Option
 */
class RedisVar[A](val name: String, default: Option[A] = None)(implicit conn: AkkaRedisClient, toBytes: (A) => Array[Byte], fromBytes: (Array[Byte]) => A) {
  protected val key = name.getBytes

  default.foreach(this.setnx)

  def isEmpty: Boolean = !isDefined

  def isDefined: Boolean = (conn send commands.getType(key)) == "string"

  def get: A = this.toOption.get

  def getFuture = new WrappedFuture(conn !!! commands.get(key))(_.map(fromBytes))

  def set(in: A): Unit = conn ! commands.set(key, toBytes(in))

  def setnx(in: A): Boolean = conn send commands.setnx(key, toBytes(in))

  def getOrElse[B >: A](default: => B): B = this.toOption.getOrElse(default)

  def orNull[A1 >: A](implicit ev: Null <:< A1): A1 = this.toOption.orNull(ev)

  def map[B](f: A => B): Option[B] = this.toOption.map(f)

  // Not transactional, yet
  def alter(f: A => A): Boolean = this.toOption.map(x => this.set(f(x))).isDefined

  def filter(p: A => Boolean): Option[A] = this.toOption.filter(p)

  def exists(p: A => Boolean): Boolean = this.filter(p).isDefined

  def foreach[U](f: A => U) = this.toOption.foreach(f)

  def collect[B](pf: PartialFunction[A, B]): Option[B] = this.toOption.collect(pf)

  def orElse[B >: A](alternative: => Option[B]): Option[B] =  this.toOption.orElse(alternative)

  def iterator: Iterator[A] = this.toOption.iterator

  def toOption: Option[A] = (conn send commands.get(key)).map(fromBytes)

  def toList: List[A] = this.toOption.toList

  def toRight[X](left: => X) = this.toOption.toRight(left)

  def toLeft[X](right: => X) = this.toOption.toLeft(right)

  override def toString: String = "RedisVar("+name+")"
}

object RedisLongVar {
  def apply(name: String, default: Option[Long] = None)(implicit conn: AkkaRedisClient): RedisLongVar = new RedisLongVar(name, default)(conn)

  implicit def toBytes(in: Long): Array[Byte] = in.toString.getBytes
  implicit def fromBytes(in: Array[Byte]): Long = (new String(in)).toLong
}

class RedisLongVar(name: String, default: Option[Long] = None)(implicit conn: AkkaRedisClient) extends RedisVar[Long](name, default)(conn, RedisLongVar.toBytes, RedisLongVar.fromBytes) {

  def incr: Long = conn send commands.incr(key)

  def incrBy(num: Long): Long = conn send commands.incrby(key, num)

  def decr: Long = conn send commands.decr(key)

  def decrBy(num: Long): Long = conn send commands.decrby(key, num)

  def incrFast: Unit = conn ! commands.incr(key)

  def incrByFast(num: Long): Unit = conn ! commands.incrby(key, num)

  def decrFast: Unit = conn ! commands.decr(key)

  def decrByFast(num: Long): Unit = conn ! commands.decrby(key, num)

  def incrFuture = conn !!! commands.incr(key)

  def incrByFuture(num: Long) = conn !!! commands.incrby(key, num)

  def decrFuture = conn !!! commands.decr(key)

  def decrByFuture(num: Long) = conn !!! commands.decrby(key, num)

  override def toString: String = "RedisLongVar("+name+")"
}
