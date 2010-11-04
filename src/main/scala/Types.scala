package net.fyrie.redis

object RedisType {
  implicit object RedisString extends RedisType[String] {
    def apply(in: Array[Byte]): String = new String(in, "UTF-8")
  }
  implicit object RedisInteger extends RedisType[Long] {
    def apply(in: Array[Byte]): Long = new String(in, "UTF-8").toLong
  }
  implicit object RedisBulk extends RedisType[Option[Array[Byte]]] {
    def apply(in: Array[Byte]): Option[Array[Byte]] = Some(in)
  }
  implicit object RedisMulti extends RedisType[Option[Int]] {
    def apply(in: Array[Byte]): Option[Int] = Some(new String(in, "UTF-8").toInt).filter(_ >= 0)
  }
  implicit object RedisError extends RedisType[Exception] {
    def apply(in: Array[Byte]): Exception = new RedisErrorException(new String(in, "UTF-8"))
  }
}

sealed trait RedisType[A] {
  def apply(in: Array[Byte]): A
}
