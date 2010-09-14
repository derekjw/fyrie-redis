package net.fyrie.redis
package commands

import helpers._
import replies._

// SADD
// Add the specified member to the set value stored at key.
case class sadd(key: Any, value: Any) extends Command[IntAsBoolean]

// SREM
// Remove the specified member from the set value stored at key.
case class srem(key: Any, value: Any) extends Command[IntAsBoolean]

// SPOP
// Remove and return (pop) a random element from the Set value at key.
case class spop(key: Any) extends Command[Bulk]

// SMOVE
// Move the specified member from one Set to another atomically.
case class smove(sourceKey: Any, destKey: Any, value: Any) extends Command[IntAsBoolean]

// SCARD
// Return the number of elements (the cardinality) of the Set at key.
case class scard(key: Any) extends Command[Int]

// SISMEMBER
// Test if the specified value is a member of the Set at key.
case class sismember(key: Any, value: Any) extends Command[IntAsBoolean]

// SINTER
// Return the intersection between the Sets stored at key1, key2, ..., keyN.
case class sinter(keys: Iterable[Any]) extends Command[MultiBulkAsSet] {
  override def args = keys.iterator
}

// SINTERSTORE
// Compute the intersection between the Sets stored at key1, key2, ..., keyN,
// and store the resulting Set at dstkey.
case class sinterstore(dstkey: Any, keys: Iterable[Any]) extends Command[Int] {
  override def args = arg1(dstkey) ++ keys.iterator
}

// SUNION
// Return the union between the Sets stored at key1, key2, ..., keyN.
case class sunion(keys: Iterable[Any]) extends Command[MultiBulkAsSet] {
  override def args = keys.iterator
}

// SUNIONSTORE
// Compute the union between the Sets stored at key1, key2, ..., keyN,
// and store the resulting Set at dstkey.
case class sunionstore(dstkey: Any, keys: Iterable[Any]) extends Command[Int] {
  override def args = arg1(dstkey) ++ keys.iterator
}

// SDIFF
// Return the difference between the Set stored at key1 and all the Sets key2, ..., keyN.
case class sdiff(keys: Iterable[Any]) extends Command[MultiBulkAsSet] {
  override def args = keys.iterator
}

// SDIFFSTORE
// Compute the difference between the Set key1 and all the Sets key2, ..., keyN,
// and store the resulting Set at dstkey.
case class sdiffstore(dstkey: Any, keys: Iterable[Any]) extends Command[Int] {
  override def args = arg1(dstkey) ++ keys.iterator
}

// SMEMBERS
// Return all the members of the Set value at key.
case class smembers(key: Any) extends Command[MultiBulkAsSet]

// SRANDMEMBER
// Return a random element from a Set
case class srandmember(key: Any) extends Command[Bulk]
