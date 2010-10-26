package net.fyrie.redis

sealed trait RedisType[A] {
  def isError: Boolean = false
  def value: A
}

object RedisString {
  def apply(in: Array[Byte]) = new RedisString(new String(in, "UTF-8"))
}
final case class RedisString(value: String) extends RedisType[String]

object RedisInteger {
  def apply(in: Array[Byte]) = new RedisInteger(new String(in, "UTF-8").toLong)
}
final case class RedisInteger(value: Long) extends RedisType[Long]

object RedisBulk {
  def apply(in: Array[Byte]) = new RedisBulk(Some(in))
}
final case class RedisBulk(value: Option[Array[Byte]]) extends RedisType[Option[Array[Byte]]]

object RedisMulti {
  def apply(in: Array[Byte]) = {
    val length = new String(in, "UTF-8").toInt
    if (length < 0) new RedisMulti(None) else new RedisMulti(Some(length))
  }
}
final case class RedisMulti(value: Option[Int]) extends RedisType[Option[Int]]

object RedisError {
  def apply(in: Array[Byte]) = new RedisError(new RedisErrorException(new String(in, "UTF-8")))
}
final case class RedisError(value: Exception) extends RedisType[Exception] {
  override def isError = true
}
