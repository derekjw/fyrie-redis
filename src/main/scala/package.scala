package net.fyrie

import akka.util.ByteString
import akka.dispatch.Future
import redis.serialization.Parse

package object redis {
  implicit def parseBulkFuture(future: Future[Option[ByteString]]) = new ParseBulkFuture(future)
  implicit def parseMultiBulkFuture(future: Future[Option[List[Option[ByteString]]]]) = {
    println(future.get)
    new ParseMultiBulkFuture(future)
  }
}

package redis {
  private[redis] class ParseBulkFuture(future: Future[Option[ByteString]]) {
    def parse[A](implicit parseA: Parse[A]): Future[Option[A]] = future.map(_.map(parseA(_)))
  }
  private[redis] class ParseMultiBulkFuture(future: Future[Option[List[Option[ByteString]]]]) {
    def parse[A](implicit parseA: Parse[A]): Future[Option[List[Option[A]]]] = future.map(_.map(_.map(_.map(parseA(_)))))
  }
}
