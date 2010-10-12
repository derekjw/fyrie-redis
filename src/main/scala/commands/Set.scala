package net.fyrie.redis
package commands

import Command._
import handlers._
import serialization._

trait SetCommands {
  // SADD
  // Add the specified member to the set value stored at key.
  case class sadd(key: Any, value: Any)(implicit format: Format) extends Command(IntAsBoolean)

  // SREM
  // Remove the specified member from the set value stored at key.
  case class srem(key: Any, value: Any)(implicit format: Format) extends Command(IntAsBoolean)

  // SPOP
  // Remove and return (pop) a random element from the Set value at key.
  case class spop[A](key: Any)(implicit format: Format, parse: Parse[A]) extends Command(Bulk[A])

  // SMOVE
  // Move the specified member from one Set to another atomically.
  case class smove(sourceKey: Any, destKey: Any, value: Any)(implicit format: Format) extends Command(IntAsBoolean)

  // SCARD
  // Return the number of elements (the cardinality) of the Set at key.
  case class scard(key: Any)(implicit format: Format) extends Command(ShortInt)

  // SISMEMBER
  // Test if the specified value is a member of the Set at key.
  case class sismember(key: Any, value: Any)(implicit format: Format) extends Command(IntAsBoolean)

  // SINTER
  // Return the intersection between the Sets stored at key1, key2, ..., keyN.
  case class sinter[A](keys: Iterable[Any])(implicit format: Format, parse: Parse[A]) extends Command(MultiBulkAsFlat[A]) {
    override def args = keys.iterator
  }

  // SINTERSTORE
  // Compute the intersection between the Sets stored at key1, key2, ..., keyN,
  // and store the resulting Set at dstkey.
  case class sinterstore(dstkey: Any, keys: Iterable[Any])(implicit format: Format) extends Command(ShortInt) {
    override def args = arg1(dstkey) ++ keys.iterator
  }

  // SUNION
  // Return the union between the Sets stored at key1, key2, ..., keyN.
  case class sunion[A](keys: Iterable[Any])(implicit format: Format, parse: Parse[A]) extends Command(MultiBulkAsFlat[A]) {
    override def args = keys.iterator
  }

  // SUNIONSTORE
  // Compute the union between the Sets stored at key1, key2, ..., keyN,
  // and store the resulting Set at dstkey.
  case class sunionstore(dstkey: Any, keys: Iterable[Any])(implicit format: Format) extends Command(ShortInt) {
    override def args = arg1(dstkey) ++ keys.iterator
  }

  // SDIFF
  // Return the difference between the Set stored at key1 and all the Sets key2, ..., keyN.
  case class sdiff[A](keys: Iterable[Any])(implicit format: Format, parse: Parse[A]) extends Command(MultiBulkAsFlat[A]) {
    override def args = keys.iterator
  }

  // SDIFFSTORE
  // Compute the difference between the Set key1 and all the Sets key2, ..., keyN,
  // and store the resulting Set at dstkey.
  case class sdiffstore(dstkey: Any, keys: Iterable[Any])(implicit format: Format) extends Command(ShortInt) {
    override def args = arg1(dstkey) ++ keys.iterator
  }

  // SMEMBERS
  // Return all the members of the Set value at key.
  case class smembers[A](key: Any)(implicit format: Format, parse: Parse[A]) extends Command(MultiBulkAsFlat[A])

  // SRANDMEMBER
  // Return a random element from a Set
  case class srandmember[A](key: Any)(implicit format: Format, parse: Parse[A]) extends Command(Bulk[A])
}
