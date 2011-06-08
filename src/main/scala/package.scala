package net.fyrie

import akka.util.ByteString
import akka.dispatch.Future
import redis.serialization.Parse

package object redis {
  implicit def parseBulkFuture(future: Future[Option[ByteString]]) = new ParseBulkFuture(future)
  implicit def parseMultiBulkFuture(future: Future[Option[List[Option[ByteString]]]]) = new ParseMultiBulkFuture(future)
  implicit def parseBulk(value: Option[ByteString]) = new ParseBulk(value)
  implicit def parseMultiBulk(value: Option[List[Option[ByteString]]]) = new ParseMultiBulk(value)
}

package redis {
  private[redis] class ParseBulkFuture(future: Future[Option[ByteString]]) {
    def parse[A](implicit parseA: Parse[A]): Future[Option[A]] = future.map(_.map(parseA(_)))
  }
  private[redis] class ParseMultiBulkFuture(future: Future[Option[List[Option[ByteString]]]]) {
    def parse[A](implicit parseA: Parse[A]): Future[Option[List[Option[A]]]] = future.map(_.map(_.map(_.map(parseA(_)))))
  }
  private[redis] class ParseBulk(value: Option[ByteString]) {
    def parse[A](implicit parseA: Parse[A]): Option[A] = value.map(parseA(_))
  }
  private[redis] class ParseMultiBulk(value: Option[List[Option[ByteString]]]) {
    def parse[A](implicit parseA: Parse[A]): Option[List[Option[A]]] = value.map(_.map(_.map(parseA(_))))
  }
}
