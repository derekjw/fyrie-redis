package net.fyrie.redis
package akka
package collection

import se.scalablesolutions.akka.dispatch._

class WrappedFuture[T, U](val future: Future[U])(implicit convert: (U) => T) {
  def await = {
    future.await
    this
  }
  def awaitBlocking = {
    future.awaitBlocking
    this
  }
  def isCompleted = future.isCompleted
  def isExpired = future.isExpired
  def timeoutInNanos = future.timeoutInNanos
  def result = future.result.map(convert)
  def exception = future.exception
}
