package net.fyrie.redis
package commands

import helpers._
import replies._

// LPUSH
// add string value to the head of the list stored at key
case class lpush(key: Any, value: Any) extends Command[Int]

// RPUSH
// add string value to the head of the list stored at key
case class rpush(key: Any, value: Any) extends Command[Int]

// LLEN
// return the length of the list stored at the specified key.
// If the key does not exist zero is returned (the same behaviour as for empty lists). 
// If the value stored at key is not a list an error is returned.
case class llen(key: Any) extends Command[Int]

// LRANGE
// return the specified elements of the list stored at the specified key.
// Start and end are zero-based indexes. 
case class lrange(key: Any, start: Int = 0, end: Int = -1) extends Command[MultiBulkAsFlat]

// LTRIM
// Trim an existing list so that it will contain only the specified range of elements specified.
case class ltrim(key: Any, start: Int = 0, end: Int = -1) extends Command[OkStatus]

// LINDEX
// return the especified element of the list stored at the specified key. 
// Negative indexes are supported, for example -1 is the last element, -2 the penultimate and so on.
case class lindex(key: Any, index: Int) extends Command[Bulk]

// LSET
// set the list element at index with the new value. Out of range indexes will generate an error
case class lset(key: Any, index: Int, value: Any) extends Command[OkStatus]

// LREM
// Remove the first count occurrences of the value element from the list.
case class lrem(key: Any, count: Int = 0, value: Any) extends Command[Int]

// LPOP
// atomically return and remove the first (LPOP) or last (RPOP) element of the list
case class lpop(key: Any) extends Command[Bulk]

// RPOP
// atomically return and remove the first (LPOP) or last (RPOP) element of the list
case class rpop(key: Any) extends Command[Bulk]

// RPOPLPUSH
// Remove the first count occurrences of the value element from the list.
case class rpoplpush(srcKey: Any, dstKey: Any) extends Command[Bulk]

case class blpop(key: Any, values: Iterable[Any], timeoutInSeconds: Int) extends Command[MultiBulk] {
  override def args = arg1(key) ++ values.iterator ++ arg1(timeoutInSeconds)
}

case class brpop(key: Any, values: Iterable[Any], timeoutInSeconds: Int) extends Command[MultiBulk] {
  override def args = arg1(key) ++ values.iterator ++ arg1(timeoutInSeconds)
}
