package net.fyrie.redis
package commands

import replies._

// SET KEY (key, value)
// sets the key with the specified value.
case class set(key: Any, value: Any) extends Command[OkStatus]

// GET (key)
// gets the value for the specified key.
case class get(key: Any) extends Command[Bulk]

// GETSET (key, value)
// is an atomic set this value and return the old value command.
case class getset(key: Any, value: Any) extends Command[Bulk]

// SETNX (key, value)
// sets the value for the specified key, only if the key is not there.
case class setnx(key: Any, value: Any) extends Command[IntAsBoolean]

// INCR (key)
// increments the specified key by 1
case class incr(key: Any) extends Command[Long]

// INCR (key, increment)
// increments the specified key by increment
case class incrby(key: Any, increment: Long) extends Command[Long]

// DECR (key)
// decrements the specified key by 1
case class decr(key: Any) extends Command[Long]

// DECR (key, increment)
// decrements the specified key by increment
case class decrby(key: Any, increment: Long) extends Command[Long]

// MGET (key, key, key, ...)
// get the values of all the specified keys.
case class mget(keys: Iterable[Any]) extends Command[MultiBulk]

// MSET (key1 value1 key2 value2 ..)
// set the respective key value pairs. Overwrite value if key exists
case class mset(kvs: Iterable[Product2[Any, Any]]) extends Command[OkStatus] {
  override def args = kvs.toStream.map(x => Seq(x._1, x._2))
}

// MSETNX (key1 value1 key2 value2 ..)
// set the respective key value pairs. Noop if any key exists
case class msetnx(kvs: Iterable[Product2[Any, Any]]) extends Command[IntAsBoolean] {
  override def args = kvs.toStream.map(x => Seq(x._1, x._2))
}
