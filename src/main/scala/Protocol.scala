package net.fyrie.redis

import akka.util.ByteString

object Protocol {
  val EOL = ByteString("\r\n")

  val KEYS = ByteString("KEYS")
  val RANDOMKEY = ByteString("RANDOMKEY")
  val FLUSHALL = ByteString("FLUSHALL")

  val GET = ByteString("GET")
  val SET = ByteString("SET")
  val GETSET = ByteString("GETSET")
  val SETNX = ByteString("SETNX")
  val INCR = ByteString("INCR")
  val INCRBY = ByteString("INCRBY")
  val DECR = ByteString("DECR")
  val DECRBY = ByteString("DECRBY")
  val MGET = ByteString("MGET")
  val MSET = ByteString("MSET")
  val MSETNX = ByteString("MSETNX")

}
