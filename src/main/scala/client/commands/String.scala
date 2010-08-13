package net.fyrie.redis
package commands

import replies._

// SET KEY (key, value)
// sets the key with the specified value.
case class set(key: Array[Byte], value: Array[Byte]) extends Command[OkStatus]

// GET (key)
// gets the value for the specified key.
case class get(key: Array[Byte]) extends Command[Bulk]

// GETSET (key, value)
// is an atomic set this value and return the old value command.
case class getset(key: Array[Byte], value: Array[Byte]) extends Command[Bulk]

// SETNX (key, value)
// sets the value for the specified key, only if the key is not there.
case class setnx(key: Array[Byte], value: Array[Byte]) extends Command[IntAsBoolean]

// INCR (key)
// increments the specified key by 1
case class incr(key: Array[Byte]) extends Command[Long]

// INCR (key, increment)
// increments the specified key by increment
case class incrby(key: Array[Byte], increment: Long) extends Command[Long]

// DECR (key)
// decrements the specified key by 1
case class decr(key: Array[Byte]) extends Command[Long]

// DECR (key, increment)
// decrements the specified key by increment
case class decrby(key: Array[Byte], increment: Long) extends Command[Long]

// MGET (key, key, key, ...)
// get the values of all the specified keys.
case class mget(keys: Iterable[Array[Byte]]) extends Command[MultiBulk] {
  override def args = keys.toSeq
}

// MSET (key1 value1 key2 value2 ..)
// set the respective key value pairs. Overwrite value if key exists
case class mset(kvs: Iterable[(Array[Byte], Array[Byte])]) extends Command[OkStatus] {
  override def args = kvs.toSeq.flatMap(kv => Seq(kv._1,kv._2))
}

// MSETNX (key1 value1 key2 value2 ..)
// set the respective key value pairs. Noop if any key exists
case class msetnx(kvs: Iterable[(Array[Byte], Array[Byte])]) extends Command[IntAsBoolean] {
  override def args = kvs.toSeq.flatMap(kv => Seq(kv._1,kv._2))
}
