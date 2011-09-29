package net.fyrie.redis
package commands

import serialization._
import akka.util.ByteString

private[redis] trait Keys[Result[_]] {
  this: Commands[Result] ⇒
  import protocol.Constants._

  /**
   * Request keys matching `pattern`.
   *
   * Returns: `Set[ByteString]`
   *
   * @param pattern Use "*" as wildcard
   *
   * @see <a href="http://code.google.com/p/redis/wiki/KeysCommand">Redis Command Reference</a>
   */
  def keys[A: Store](pattern: A): Result[Set[ByteString]] = send(KEYS :: Store(pattern) :: Nil)
  def keys(): Result[Set[ByteString]] = send(KEYS :: ALLKEYS :: Nil)

  /**
   * Request a random key.
   *
   * Returns: `Option[ByteString]`
   *
   * @see <a href="http://code.google.com/p/redis/wiki/RandomkeyCommand">Redis Command Reference</a>
   */
  def randomkey(): Result[Option[ByteString]] = send(List(RANDOMKEY))

  /**
   * Rename a key.
   *
   * Returns: `Unit`
   *
   * @param oldkey The existing key to rename.
   * @param newkey The new key.
   *
   * @see <a href="http://code.google.com/p/redis/wiki/RenameCommand">Redis Command Reference</a>
   */
  def rename[A: Store, B: Store](oldkey: A, newkey: B): Result[Unit] =
    send(RENAME :: Store(oldkey) :: Store(newkey) :: Nil)

  /**
   * Rename a key if `newkey` does not already exist. Returns true if
   * successfully renamed.
   *
   * Returns: `Boolean`
   *
   * @param oldkey The existing key to rename.
   * @param newkey The new key.
   *
   * @see <a href="http://code.google.com/p/redis/wiki/RenamenxCommand">Redis Command Reference</a>
   */
  def renamenx[A: Store, B: Store](oldkey: A, newkey: B): Result[Boolean] =
    send(RENAMENX :: Store(oldkey) :: Store(newkey) :: Nil)

  /**
   * Request the number of keys in the current database.
   *
   * Returns: `Int`
   *
   * @see <a href="http://code.google.com/p/redis/wiki/DbsizeCommand">Redis Command Reference</a>
   */
  def dbsize(): Result[Int] = send(DBSIZE :: Nil)

  /**
   * Tests if `key` exists.
   *
   * Returns: `Boolean`
   *
   * @param key The key to test the existance of.
   *
   * @see <a href="http://code.google.com/p/redis/wiki/ExistsCommand">Redis Command Reference</a>
   */
  def exists[A: Store](key: A): Result[Boolean] = send(EXISTS :: Store(key) :: Nil)

  /**
   * Delete each key in `keys`. Returns the actual number of keys deleted.
   *
   * Note: Be aware that this command takes an `Iterable[Any]`, so if a
   * single `String` is passed it will be converted into a `Seq[Char]`
   * which is probably not what you want. Instead, provide a `List[String]`
   * or similar collection.
   *
   * Returns: `Int`
   *
   * @param keys A collection of keys to delete.
   *
   * @see <a href="http://code.google.com/p/redis/wiki/DelCommand">Redis Command Reference</a>
   */
  def del[A: Store](keys: Iterable[A]): Result[Int] = send(DEL :: (keys.map(Store(_))(collection.breakOut): List[ByteString]))
  def del[A: Store](key: A): Result[Int] = send(DEL :: Store(key) :: Nil)

  /**
   * Requests the type of the value stored at `key`.
   *
   * Returns: `String`
   *
   * @param key The key of the value to check.
   *
   * @see <a href="http://code.google.com/p/redis/wiki/TypeCommand">Redis Command Reference</a>
   */
  def typeof[A: Store](key: A): Result[String] = send(TYPE :: Store(key) :: Nil)

  /**
   * Set a timeout of the specified key. After the timeout the key will be
   * automatically deleted. Returns 'true' if the expire command is successful.
   *
   * Returns: `Boolean`
   *
   * @param key     The key to expire.
   * @param seconds The timeout in seconds.
   *
   * @see <a href="http://code.google.com/p/redis/wiki/ExpireCommand">Redis Command Reference</a>
   */
  def expire[A: Store](key: A, seconds: Long): Result[Boolean] =
    send(EXPIRE :: Store(key) :: Store(seconds) :: Nil)

  /**
   * Set a timeout of the specified key. After the timeout the key will be
   * automatically deleted. Returns 'true' if the expire command is successful.
   *
   * Returns: `Boolean`
   *
   * @param key      The key to expire.
   * @param unixtime The timeout in the form of a UNIX timestamp.
   *
   * @see <a href="http://code.google.com/p/redis/wiki/ExpireCommand">Redis Command Reference</a>
   */
  def expireat[A: Store](key: A, unixtime: Long): Result[Boolean] =
    send(EXPIREAT :: Store(key) :: Store(unixtime) :: Nil)

  /**
   * Select a DB with the supplied zero-based index.
   *
   * Returns: `Unit`
   *
   * @param index Zero-based index of database.
   *
   * @see <a href="http://code.google.com/p/redis/wiki/SelectCommand">Redis Command Reference</a>
   */
  def select(index: Int = 0): Result[Unit] = send(SELECT :: Store(index) :: Nil)

  /**
   * Delete all keys in the currently selected database.
   *
   * Returns: `Unit`
   *
   * @see <a href="http://code.google.com/p/redis/wiki/FlushdbCommand">Redis Command Reference</a>
   */
  def flushdb(): Result[Unit] = send(FLUSHDB :: Nil)

  /**
   * Delete all keys in all databases.
   *
   * Returns: `Unit`
   *
   * @see <a href="http://code.google.com/p/redis/wiki/FlushallCommand">Redis Command Reference</a>
   */
  def flushall(): Result[Unit] = send(FLUSHALL :: Nil)

  /**
   * Move `key` from the currently selected database to the database at index `db`.
   * Returns `true` if successful.
   *
   * Returns: `Boolean`
   *
   * @param key The key to move.
   * @param db  The zero-based index of the destination database.
   *
   * @see <a href="http://code.google.com/p/redis/wiki/MoveCommand">Redis Command Reference</a>
   */
  def move[A: Store](key: A, db: Int = 0): Result[Boolean] = send(MOVE :: Store(key) :: Store(db) :: Nil)

  /**
   * Asks the server to close the connection.
   *
   * Returns: `Unit`
   *
   * @see <a href="http://code.google.com/p/redis/wiki/QuitCommand">Redis Command Reference</a>
   */
  def quit(): Result[Unit] = send(QUIT :: Nil)

  /**
   * Supply a password if required to send commands.
   *
   * Returns: `Unit`
   *
   * @param secret Server authentication password.
   *
   * @see <a href="http://code.google.com/p/redis/wiki/AuthCommand">Redis Command Reference</a>
   */
  def auth[A: Store](secret: A): Result[Unit] = send(AUTH :: Store(secret) :: Nil)

  def ping(): Result[String] = send(PING :: Nil)

  def echo[A: Store](value: A): Result[Option[ByteString]] = send(ECHO :: Store(value) :: Nil)

  /**
   * Sorts the elements contained in a List, Set, or Sorted Set value at `key`.
   *
   * Returns: `List[Option[A]]`
   *
   * @param key   The key of the List, Set, or Sorted Set to sort.
   * @param by    Optional pattern used to generate the key names of the weights used for sorting.
   *              Use "nosort" if no sorting is required, which can be useful if using
   *              the `get` parameter to return other values.
   * @param limit Optional zero-based start-index and count of items to return.
   * @param get   List of patterns used to generate key names of values to return.
   *              Use "#" to include the elements of the sorted list as well.
   * @param order Optional `SortOrder`
   * @param alpha If `true`, sort lexicalgraphically.
   *
   * @see <a href="http://code.google.com/p/redis/wiki/SortCommand">Redis Command Reference</a>
   */
  def sort[K: Store, B: Store, G: Store](key: K, by: Option[B] = Option.empty[ByteString], limit: RedisLimit = NoLimit, get: Seq[G] = List.empty[ByteString], order: Option[SortOrder] = None, alpha: Boolean = false): Result[List[Option[ByteString]]] = {
    var cmd: List[ByteString] = Nil
    if (alpha) cmd ::= ALPHA
    order foreach (o ⇒ cmd ::= Store(o))
    get.reverse foreach (g ⇒ cmd = GET :: Store(g) :: cmd)
    limit match {
      case Limit(o, c) ⇒ cmd = LIMIT :: Store(o) :: Store(c) :: cmd
      case NoLimit     ⇒
    }
    by foreach (b ⇒ cmd = BY :: Store(b) :: cmd)
    send(SORT :: Store(key) :: cmd)
  }

}

