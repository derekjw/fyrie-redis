package net.fyrie.redis
package commands

import serialization._
import akka.util.ByteString

trait Strings {
  this: Commands =>
  import Protocol._

  def set[K: Store,V: Store](key: K, value: V): Result[Unit] =
    send(SET :: Store(key) :: Store(value) :: Nil)

  def get[K: Store](key: K): Result[Option[ByteString]] =
    send(GET :: Store(key) :: Nil)

  def getset[K: Store, V: Store](key: K, value: V): Result[Option[ByteString]] =
    send(GETSET :: Store(key) :: Store(value) :: Nil)

  def setnx[K: Store, V: Store](key: K, value: V): Result[Boolean] =
    send(SETNX :: Store(key) :: Store(value) :: Nil)

  def incr[K: Store](key: K): Result[Long] =
    send(INCR :: Store(key) :: Nil)

  def incrby[K: Store](key: K, increment: Long): Result[Long] =
    send(INCRBY :: Store(key) :: Store(increment) :: Nil)

  def decr[K: Store](key: K): Result[Long] =
    send(DECR :: Store(key) :: Nil)

  def decrby[K: Store](key: K, decrement: Long): Result[Long] =
    send(DECRBY :: Store(key) :: Store(decrement) :: Nil)

  def mget[K: Store](keys: Seq[K]): Result[List[Option[ByteString]]] =
    send(MGET :: (keys.map(Store(_))(collection.breakOut): List[ByteString]) )

  def mset[K: Store, V: Store](kvs: Iterable[Product2[K, V]]): Result[Unit] =
    send(MSET :: (kvs.flatMap(kv => Iterable(Store(kv._1), Store(kv._2)))(collection.breakOut): List[ByteString]) )

  def msetnx[K: Store, V: Store](kvs: Iterable[Product2[K, V]]): Result[Boolean] =
    send(MSETNX :: (kvs.flatMap(kv => Iterable(Store(kv._1), Store(kv._2)))(collection.breakOut): List[ByteString]) )
}

