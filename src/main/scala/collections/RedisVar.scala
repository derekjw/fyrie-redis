package net.fyrie.redis
package akka
package collection

import serialization._

import se.scalablesolutions.akka.dispatch.{Future}

object RedisVar {
  def apply[A](name: String, default: Option[A] = None)(implicit conn: AkkaRedisClient, format: Format, parser: Parse[A]): RedisVar[A] =
    new RedisVar[A](name, default)(conn, format, parser)
}

/**  Modeled to be similar to Option
 */
class RedisVar[A](val name: String, default: Option[A] = None)(implicit conn: AkkaRedisClient, format: Format, parser: Parse[A]) {
  protected val key = name.getBytes

  default.foreach(this.setnx)

  def isEmpty: Boolean = !isDefined

  def isDefined: Boolean = (conn send Commands.getType(key)) == "string"

  def get: A = this.toOption.get

  def getFuture: Future[Result[A]] = conn !!! Commands.get(key)

  def set(in: A): Unit = conn ! Commands.set(key, in)

  def setnx(in: A): Boolean = conn send Commands.setnx(key, in)

  def getOrElse[B >: A](default: => B): B = this.toOption.getOrElse(default)

  def orNull[A1 >: A](implicit ev: Null <:< A1): A1 = this.toOption.orNull(ev)

  def map[B](f: A => B): Option[B] = this.toOption.map(f)

  def mapFuture[B](f: A => B): Future[Result[B]] = {
    implicit val parserB: Parse[B] = Parse[B](x => f(parser(x)))
    conn !!! Commands.get[B](key)
  }

  // Not transactional, yet
  def alter(f: A => A): Boolean = this.toOption.map(x => this.set(f(x))).isDefined

  def filter(p: A => Boolean): Option[A] = this.toOption.filter(p)

  def exists(p: A => Boolean): Boolean = this.filter(p).isDefined

  def foreach[U](f: A => U) = this.toOption.foreach(f)

  def collect[B](pf: PartialFunction[A, B]): Option[B] = this.toOption.collect(pf)

  def orElse[B >: A](alternative: => Option[B]): Option[B] =  this.toOption.orElse(alternative)

  def iterator: Iterator[A] = this.toOption.iterator

  def toOption: Option[A] = conn send Commands.get(key)

  def toList: List[A] = this.toOption.toList

  def toRight[X](left: => X) = this.toOption.toRight(left)

  def toLeft[X](right: => X) = this.toOption.toLeft(right)

  override def toString: String = "RedisVar("+name+")"
}

object RedisLongVar {
  def apply(name: String, default: Option[Long] = None)(implicit conn: AkkaRedisClient): RedisLongVar = new RedisLongVar(name, default)(conn)
}

class RedisLongVar(name: String, default: Option[Long] = None)(implicit conn: AkkaRedisClient) extends RedisVar[Long](name, default)(conn, Format.default, Parse.Implicits.parseLong) {

  def incr: Long = conn send Commands.incr(key)

  def incrBy(num: Long): Long = conn send Commands.incrby(key, num)

  def decr: Long = conn send Commands.decr(key)

  def decrBy(num: Long): Long = conn send Commands.decrby(key, num)

  def incrFast: Unit = conn ! Commands.incr(key)

  def incrByFast(num: Long): Unit = conn ! Commands.incrby(key, num)

  def decrFast: Unit = conn ! Commands.decr(key)

  def decrByFast(num: Long): Unit = conn ! Commands.decrby(key, num)

  def incrFuture = conn !!! Commands.incr(key)

  def incrByFuture(num: Long) = conn !!! Commands.incrby(key, num)

  def decrFuture = conn !!! Commands.decr(key)

  def decrByFuture(num: Long) = conn !!! Commands.decrby(key, num)

  override def toString: String = "RedisLongVar("+name+")"
}
