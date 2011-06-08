/*package net.fyrie.redis
package commands

import handlers._
import serialization._
import Command._

trait ListCommands {
  // LPUSH
  // add string value to the head of the list stored at key
  case class lpush(key: Any, value: Any)(implicit format: Format) extends Command(ShortInt)

  // RPUSH
  // add string value to the head of the list stored at key
  case class rpush(key: Any, value: Any)(implicit format: Format) extends Command(ShortInt)

  // LLEN
  // return the length of the list stored at the specified key.
  // If the key does not exist zero is returned (the same behaviour as for empty lists).
  // If the value stored at key is not a list an error is returned.
  case class llen(key: Any)(implicit format: Format) extends Command(ShortInt)

  // LRANGE
  // return the specified elements of the list stored at the specified key.
  // Start and end are zero-based indexes.
  case class lrange[A](key: Any, start: Int = 0, end: Int = -1)(implicit format: Format, parse: Parse[A]) extends Command(MultiBulkAsFlat[A]()(implicitly, parse.manifest))

  // LTRIM
  // Trim an existing list so that it will contain only the specified range of elements specified.
  case class ltrim(key: Any, start: Int = 0, end: Int = -1)(implicit format: Format) extends Command(OkStatus)

  // LINDEX
  // return the especified element of the list stored at the specified key.
  // Negative indexes are supported, for example -1 is the last element, -2 the penultimate and so on.
  case class lindex[A](key: Any, index: Int)(implicit format: Format, parse: Parse[A]) extends Command(Bulk[A]()(implicitly, parse.manifest))

  // LSET
  // set the list element at index with the new value. Out of range indexes will generate an error
  case class lset(key: Any, index: Int, value: Any)(implicit format: Format) extends Command(OkStatus)

  // LREM
  // Remove the first count occurrences of the value element from the list.
  case class lrem(key: Any, count: Int = 0, value: Any)(implicit format: Format) extends Command(ShortInt)

  // LPOP
  // atomically return and remove the first (LPOP) or last (RPOP) element of the list
  case class lpop[A](key: Any)(implicit format: Format, parse: Parse[A]) extends Command(Bulk[A]()(implicitly, parse.manifest))

  // RPOP
  // atomically return and remove the first (LPOP) or last (RPOP) element of the list
  case class rpop[A](key: Any)(implicit format: Format, parse: Parse[A]) extends Command(Bulk[A]()(implicitly, parse.manifest))

  // RPOPLPUSH
  // Remove the first count occurrences of the value element from the list.
  case class rpoplpush[A](srcKey: Any, dstKey: Any)(implicit format: Format, parse: Parse[A]) extends Command(Bulk[A]()(implicitly, parse.manifest))

  case class blpop[A](key: Any, values: Iterable[Any], timeoutInSeconds: Int)(implicit format: Format, parse: Parse[A]) extends Command(MultiBulk[A]()(implicitly, parse.manifest)) {
    override def args = arg1(key) ++ values.iterator ++ arg1(timeoutInSeconds)
  }

  case class brpop[A](key: Any, values: Iterable[Any], timeoutInSeconds: Int)(implicit format: Format, parse: Parse[A]) extends Command(MultiBulk[A]()(implicitly, parse.manifest)) {
    override def args = arg1(key) ++ values.iterator ++ arg1(timeoutInSeconds)
  }
}
*/
