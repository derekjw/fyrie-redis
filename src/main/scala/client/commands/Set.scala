package net.fyrie.redis
package commands

import replies._

// SADD
// Add the specified member to the set value stored at key.
case class sadd(key: Array[Byte], value: Array[Byte]) extends Command[IntAsBoolean]

// SREM
// Remove the specified member from the set value stored at key.
case class srem(key: Array[Byte], value: Array[Byte]) extends Command[IntAsBoolean]

// SPOP
// Remove and return (pop) a random element from the Set value at key.
case class spop(key: Array[Byte]) extends Command[Bulk]

// SMOVE
// Move the specified member from one Set to another atomically.
case class smove(sourceKey: Array[Byte], destKey: Array[Byte], value: Array[Byte]) extends Command[IntAsBoolean]

// SCARD
// Return the number of elements (the cardinality) of the Set at key.
case class scard(key: Array[Byte]) extends Command[Int]

// SISMEMBER
// Test if the specified value is a member of the Set at key.
case class sismember(key: Array[Byte], value: Array[Byte]) extends Command[IntAsBoolean]

// SINTER
// Return the intersection between the Sets stored at key1, key2, ..., keyN.
case class sinter(keys: Iterable[Array[Byte]]) extends Command[MultiBulkAsSet] {
  override def args = keys.toSeq
}

// SINTERSTORE
// Compute the intersection between the Sets stored at key1, key2, ..., keyN,
// and store the resulting Set at dstkey.
case class sinterstore(keys: Iterable[Array[Byte]]) extends Command[Int] {
  override def args = keys.toSeq
}

// SUNION
// Return the union between the Sets stored at key1, key2, ..., keyN.
case class sunion(keys: Iterable[Array[Byte]]) extends Command[MultiBulkAsSet] {
  override def args = keys.toSeq
}

// SUNIONSTORE
// Compute the union between the Sets stored at key1, key2, ..., keyN,
// and store the resulting Set at dstkey.
case class sunionstore(keys: Iterable[Array[Byte]]) extends Command[Int] {
  override def args = keys.toSeq
}

// SDIFF
// Return the difference between the Set stored at key1 and all the Sets key2, ..., keyN.
case class sdiff(keys: Iterable[Array[Byte]]) extends Command[MultiBulkAsSet] {
  override def args = keys.toSeq
}

// SDIFFSTORE
// Compute the difference between the Set key1 and all the Sets key2, ..., keyN,
// and store the resulting Set at dstkey.
case class sdiffstore(keys: Iterable[Array[Byte]]) extends Command[Int] {
  override def args = keys.toSeq
}

// SMEMBERS
// Return all the members of the Set value at key.
case class smembers(key: Array[Byte]) extends Command[MultiBulkAsSet]

// SRANDMEMBER
// Return a random element from a Set
case class srandmember(key: Array[Byte]) extends Command[Bulk]
