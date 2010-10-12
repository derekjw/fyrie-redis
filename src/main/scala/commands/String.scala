package net.fyrie.redis
package commands

import Command._
import handlers._
import serialization._

trait StringCommands {
  // SET KEY (key, value)
  // sets the key with the specified value.
  case class set(key: Any, value: Any)(implicit format: Format) extends Command(OkStatus)

  // GET (key)
  // gets the value for the specified key.
  case class get[A](key: Any)(implicit format: Format, parse: Parse[A]) extends Command(Bulk[A])

  // GETSET (key, value)
  // is an atomic set this value and return the old value command.
  case class getset[A](key: Any, value: Any)(implicit format: Format, parse: Parse[A]) extends Command(Bulk[A])

  // SETNX (key, value)
  // sets the value for the specified key, only if the key is not there.
  case class setnx(key: Any, value: Any)(implicit format: Format) extends Command(IntAsBoolean)

  // INCR (key)
  // increments the specified key by 1
  case class incr(key: Any)(implicit format: Format) extends Command(LongInt)

  // INCR (key, increment)
  // increments the specified key by increment
  case class incrby(key: Any, increment: Long)(implicit format: Format) extends Command(LongInt)

  // DECR (key)
  // decrements the specified key by 1
  case class decr(key: Any)(implicit format: Format) extends Command(LongInt)

  // DECR (key, increment)
  // decrements the specified key by increment
  case class decrby(key: Any, increment: Long)(implicit format: Format) extends Command(LongInt)

  // MGET (key, key, key, ...)
  // get the values of all the specified keys.
  case class mget[A](keys: Iterable[Any])(implicit format: Format, parse: Parse[A]) extends Command(MultiBulk[A]) {
    override def args = keys.iterator
  }

  // MSET (key1 value1 key2 value2 ..)
  // set the respective key value pairs. Overwrite value if key exists
  case class mset(kvs: Iterable[Product2[Any, Any]])(implicit format: Format) extends Command(OkStatus) {
    override def args = argN2(kvs)
  }

  // MSETNX (key1 value1 key2 value2 ..)
  // set the respective key value pairs. Noop if any key exists
  case class msetnx(kvs: Iterable[Product2[Any, Any]])(implicit format: Format) extends Command(IntAsBoolean) {
    override def args = argN2(kvs)
  }
}
