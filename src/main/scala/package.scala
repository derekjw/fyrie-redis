package net.fyrie

import akka.util.ByteString
import akka.dispatch.Future
import redis.serialization.Parse

package object redis {
  implicit def parseBulkFuture(future: Future[Option[ByteString]]) = new ParseBulkFuture(future)
  implicit def parseMultiBulkFuture(future: Future[Option[List[Option[ByteString]]]]) = new ParseMultiBulkFuture(future)
  implicit def parseMultiBulkListFuture(future: Future[List[Option[ByteString]]]) = new ParseMultiBulkListFuture(future)
  implicit def parseMultiBulkFlatFuture(future: Future[Option[List[ByteString]]]) = new ParseMultiBulkFlatFuture(future)
  implicit def parseMultiBulkSetFuture(future: Future[Set[ByteString]]) = new ParseMultiBulkSetFuture(future)

  implicit def parseBulk(value: Option[ByteString]) = new ParseBulk(value)
  implicit def parseMultiBulk(value: Option[List[Option[ByteString]]]) = new ParseMultiBulk(value)
  implicit def parseMultiBulkList(value: List[Option[ByteString]]) = new ParseMultiBulkList(value)
  implicit def parseMultiBulkFlat(value: Option[List[ByteString]]) = new ParseMultiBulkFlat(value)
  implicit def parseMultiBulkSet(value: Set[ByteString]) = new ParseMultiBulkSet(value)
  implicit def parseMultiBulkMap(value: Map[ByteString, ByteString]) = new ParseMultiBulkMap(value)
}

package redis {
  private[redis] class ParseBulkFuture(future: Future[Option[ByteString]]) {
    def parse[A: Parse]: Future[Option[A]] = future.map(_.map(Parse(_)))
  }
  private[redis] class ParseMultiBulkFuture(future: Future[Option[List[Option[ByteString]]]]) {
    def parse[A: Parse]: Future[Option[List[Option[A]]]] = future.map(_.map(_.map(_.map(Parse(_)))))
  }
  private[redis] class ParseMultiBulkListFuture(future: Future[List[Option[ByteString]]]) {
    def parse[A: Parse]: Future[List[Option[A]]] = future.map(_.map(_.map(Parse(_))))
  }
  private[redis] class ParseMultiBulkFlatFuture(future: Future[Option[List[ByteString]]]) {
    def parse[A: Parse]: Future[Option[List[A]]] = future.map(_.map(_.map(Parse(_))))
  }
  private[redis] class ParseMultiBulkSetFuture(future: Future[Set[ByteString]]) {
    def parse[A: Parse]: Future[Set[A]] = future.map(_.map(Parse(_)))
  }
  private[redis] class ParseMultiBulkMapFuture(future: Future[Map[ByteString, ByteString]]) {
    def parse[K: Parse, V: Parse]: Future[Map[K, V]] = future.map(_.map(kv => (Parse[K](kv._1), Parse[V](kv._2))))
  }

  private[redis] class ParseBulk(value: Option[ByteString]) {
    def parse[A: Parse]: Option[A] = value.map(Parse(_))
  }
  private[redis] class ParseMultiBulk(value: Option[List[Option[ByteString]]]) {
    def parse[A: Parse]: Option[List[Option[A]]] = value.map(_.map(_.map(Parse(_))))
  }
  private[redis] class ParseMultiBulkList(value: List[Option[ByteString]]) {
    def parse[A: Parse]: List[Option[A]] = value.map(_.map(Parse(_)))
  }
  private[redis] class ParseMultiBulkFlat(value: Option[List[ByteString]]) {
    def parse[A: Parse]: Option[List[A]] = value.map(_.map(Parse(_)))
  }
  private[redis] class ParseMultiBulkSet(value: Set[ByteString]) {
    def parse[A: Parse]: Set[A] = value.map(Parse(_))
  }
  private[redis] class ParseMultiBulkMap(value: Map[ByteString, ByteString]) {
    def parse[K: Parse, V: Parse]: Map[K, V] = value.map(kv => (Parse[K](kv._1), Parse[V](kv._2)))
  }
}
