package net.fyrie.redis
package commands

import serialization._
import akka.util.ByteString

private[redis] trait Lists[Result[_]] {
  this: Commands[Result] ⇒
  import protocol.Constants._

  def blpop[K: Store: Parse](keys: Seq[K], timeout: Int = 0): Result[Option[(K, ByteString)]] =
    send(BLPOP :: (Store(timeout) :: (keys.map(Store(_))(collection.breakOut): List[ByteString])).reverse)

  def brpop[K: Store: Parse](keys: Seq[K], timeout: Int = 0): Result[Option[(K, ByteString)]] =
    send(BRPOP :: (Store(timeout) :: (keys.map(Store(_))(collection.breakOut): List[ByteString])).reverse)

  def brpoplpush[A: Store, B: Store](srcKey: A, dstKey: B, timeout: Int = 0): Result[Option[ByteString]] =
    send(BRPOPLPUSH :: Store(srcKey) :: Store(dstKey) :: Store(timeout) :: Nil)

  def lindex[K: Store](key: K, index: Int): Result[Option[ByteString]] =
    send(LINDEX :: Store(key) :: Store(index) :: Nil)

  def linsertbefore[K: Store, A: Store, B: Store](key: K, pivot: A, value: B): Result[Option[Int]] =
    send(LINSERT :: Store(key) :: BEFORE :: Store(pivot) :: Store(value) :: Nil)

  def linsertafter[K: Store, A: Store, B: Store](key: K, pivot: A, value: B): Result[Option[Int]] =
    send(LINSERT :: Store(key) :: AFTER :: Store(pivot) :: Store(value) :: Nil)

  def llen[K: Store](key: K): Result[Int] =
    send(LLEN :: Store(key) :: Nil)

  def lpop[K: Store](key: K): Result[Option[ByteString]] =
    send(LPOP :: Store(key) :: Nil)

  def lpush[K: Store, V: Store](key: K, value: V): Result[Int] =
    send(LPUSH :: Store(key) :: Store(value) :: Nil)

  def lpushx[K: Store, V: Store](key: K, value: V): Result[Int] =
    send(LPUSHX :: Store(key) :: Store(value) :: Nil)

  def lrange[K: Store](key: K, start: Int = 0, end: Int = -1): Result[Option[List[ByteString]]] =
    send(LRANGE :: Store(key) :: Store(start) :: Store(end) :: Nil)

  def lrem[K: Store, V: Store](key: K, value: V, count: Int = 0): Result[Int] =
    send(LREM :: Store(key) :: Store(count) :: Store(value) :: Nil)

  def lset[K: Store, V: Store](key: K, index: Int, value: V): Result[Unit] =
    send(LSET :: Store(key) :: Store(index) :: Store(value) :: Nil)

  def ltrim[K: Store](key: K, start: Int = 0, end: Int = -1): Result[Unit] =
    send(LTRIM :: Store(key) :: Store(start) :: Store(end) :: Nil)

  def rpop[K: Store](key: K): Result[Option[ByteString]] =
    send(RPOP :: Store(key) :: Nil)

  def rpoplpush[A: Store, B: Store](srcKey: A, dstKey: B): Result[Option[ByteString]] =
    send(RPOPLPUSH :: Store(srcKey) :: Store(dstKey) :: Nil)

  def rpush[K: Store, V: Store](key: K, value: V): Result[Int] =
    send(RPUSH :: Store(key) :: Store(value) :: Nil)

  def rpushx[K: Store, V: Store](key: K, value: V): Result[Int] =
    send(RPUSHX :: Store(key) :: Store(value) :: Nil)
}
