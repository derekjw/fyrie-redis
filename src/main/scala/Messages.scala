package net.fyrie
package redis
package akka
package messages

import handlers.{Handler}

trait Message

case class Request(bytes: Array[Byte], handler: Handler[_]) extends Message
