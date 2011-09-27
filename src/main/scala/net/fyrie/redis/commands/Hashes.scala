package net.fyrie.redis
package commands

import serialization._
import akka.util.ByteString

private[redis] trait Hashes[Result[_]] {
  this: Commands[Result] ⇒
  import protocol.Constants._

  def hdel[K: Store, F: Store](key: K, field: F): Result[Boolean] =
    send(HDEL :: Store(key) :: Store(field) :: Nil)

  def hexists[K: Store, F: Store](key: K, field: F): Result[Boolean] =
    send(HEXISTS :: Store(key) :: Store(field) :: Nil)

  def hget[K: Store, F: Store](key: K, field: F): Result[Option[ByteString]] =
    send(HGET :: Store(key) :: Store(field) :: Nil)

  def hgetall[K: Store](key: K): Result[Map[ByteString, ByteString]] =
    send(HGETALL :: Store(key) :: Nil)

  def hincrby[K: Store, F: Store](key: K, field: F, value: Long = 1): Result[Long] =
    send(HINCRBY :: Store(key) :: Store(field) :: Store(value) :: Nil)

  def hkeys[K: Store](key: K): Result[Set[ByteString]] =
    send(HKEYS :: Store(key) :: Nil)

  def hlen[K: Store](key: K): Result[Long] =
    send(HLEN :: Store(key) :: Nil)

  def hmget[K: Store, F: Store](key: K, fields: Seq[F]): Result[List[Option[ByteString]]] =
    send(HMGET :: Store(key) :: (fields.map(Store(_))(collection.breakOut): List[ByteString]))

  def hmset[K: Store, F: Store, V: Store](key: K, fvs: Iterable[Product2[F, V]]): Result[Unit] =
    send(HMSET :: Store(key) :: (fvs.flatMap(fv ⇒ Iterable(Store(fv._1), Store(fv._2)))(collection.breakOut): List[ByteString]))

  def hset[K: Store, F: Store, V: Store](key: K, field: F, value: V): Result[Boolean] =
    send(HSET :: Store(key) :: Store(field) :: Store(value) :: Nil)

  def hsetnx[K: Store, F: Store, V: Store](key: K, field: F, value: V): Result[Boolean] =
    send(HSETNX :: Store(key) :: Store(field) :: Store(value) :: Nil)

  def hvals[K: Store](key: K): Result[Set[ByteString]] =
    send(HVALS :: Store(key) :: Nil)

}

