package net.fyrie.redis
package protocol

import types._
import akka.util.ByteString
import akka.actor.IO

private[redis] object Iteratees {
  import Constants.EOL

  final val readUntilEOL = IO takeUntil EOL

  final val readType = (_: ByteString).head match {
    case 43 /* '+' */ ⇒ readString
    case 45 /* '-' */ ⇒ readError
    case 58 /* ':' */ ⇒ readInteger
    case 36 /* '$' */ ⇒ readBulk
    case 42 /* '*' */ ⇒ readMulti
    case x            ⇒ IO throwErr RedisProtocolException("Invalid result type: " + x)
  }

  final val readResult: IO.Iteratee[RedisType] = IO take 1 flatMap readType

  final val bytesToString = (bytes: ByteString) ⇒ RedisString(bytes.utf8String)
  final val readString: IO.Iteratee[RedisString] = readUntilEOL map bytesToString

  final val bytesToError = (bytes: ByteString) ⇒ RedisError(bytes.utf8String)
  final val readError: IO.Iteratee[RedisError] = readUntilEOL map bytesToError

  final val bytesToInteger = (bytes: ByteString) ⇒ RedisInteger(bytes.utf8String.toLong)
  final val readInteger: IO.Iteratee[RedisInteger] = readUntilEOL map bytesToInteger

  final val notFoundBulk = IO Done RedisBulk.notfound
  final val emptyBulk = IO Done RedisBulk.empty
  final val bytesToBulk = (_: ByteString).utf8String.toInt match {
    case -1 ⇒ notFoundBulk
    case 0  ⇒ emptyBulk
    case n  ⇒ for (bytes ← IO take n; _ ← readUntilEOL) yield RedisBulk(Some(bytes))
  }
  final val readBulk: IO.Iteratee[RedisBulk] = readUntilEOL flatMap bytesToBulk

  final val notFoundMulti = IO Done RedisMulti.notfound
  final val emptyMulti = IO Done RedisMulti.empty
  final val bytesToMulti = (bytes: ByteString) ⇒ bytes.utf8String.toInt match {
    case -1 ⇒ notFoundMulti
    case 0  ⇒ emptyMulti
    case n  ⇒ IO.takeList(n)(readResult) map (x ⇒ RedisMulti(Some(x)))
  }
  final val readMulti: IO.Iteratee[RedisMulti] = readUntilEOL flatMap bytesToMulti

}
