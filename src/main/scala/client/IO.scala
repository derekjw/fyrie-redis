package net.fyrie.redis

import java.io._
import java.net.Socket

import scala.collection.mutable.ArrayBuilder

case class RedisErrorException(message: String) extends RuntimeException(message)
case class RedisProtocolException(message: String) extends RuntimeException(message)
case class RedisConnectionException(message: String) extends RuntimeException(message)

trait IO {
  val host: String
  val port: Int

  protected var socket: Option[Socket] = None
  protected val os = new StreamWriter
  protected val is = new StreamReader
  protected var db: Int = 0

  def connected = socket.isDefined

  def reconnect = disconnect && connect

  // Connects the socket, and sets the input and output streams.
  def connect: Boolean = {
    try {
      socket = Some(new Socket(host, port))
      socket foreach {s =>
        s.setSoTimeout(0)
        s.setKeepAlive(true)
        os.stream = Some(s.getOutputStream)
        is.stream = Some(new BufferedInputStream(s.getInputStream))
      }
      true
    } catch {
      case x =>
        clearFd
        throw new RuntimeException(x)
    }
  }

  // Disconnects the socket.
  def disconnect: Boolean = {
    try {
      socket foreach (_.close)
      os.close
      is.close
      clearFd
      true
    } catch {
      case x =>
        false
    }
  }

  def clearFd = {
    socket = None
    os.clear()
    is.clear()
  }

  def reader: RedisStreamReader = is

  def writer: RedisStreamWriter = os

  class StreamReader extends RedisStreamReader {
    var stream: Option[InputStream] = None

    private val crlf = List(13,10)

    def read[T](handler: Reply[T]): T = {
      if(!connected) connect;
      stream.map{is =>
        var delimiter = crlf
        var found: List[Int] = Nil
        var build = new ArrayBuilder.ofByte
        while (delimiter != Nil) {
          val next = is.read
          if (next == delimiter.head) {
            found ::= delimiter.head
            delimiter = delimiter.tail
          } else {
            if (found != Nil) {
              delimiter = crlf
              build ++= found.reverseMap(_.toByte)
              found = Nil
            }
            build += next.toByte
          }
        }

        // TODO: Refactor this stuff somewhere else
        val arr = build.result
        val m = arr(0).toChar
        val s = new String(arr, 1, arr.size - 1)
        if (m == handler.marker) {
          handler.parse(this, s)
        } else {
          if (m == '-') (throw new RedisErrorException(s))
          reconnect
          throw new RedisProtocolException("Got '" + m + s + "' as initial reply byte")
        }
      }.get
    }

    def readBulk(count: Int): Array[Byte] = {
      if(!connected) connect;
      stream.map{is =>
        val arr = new Array[Byte](count)
        var cur = 0
        while (cur < count) {
          cur += is.read(arr, cur, count - cur)
        }
        if (is.read() != 13) throw new RedisProtocolException("Expected newline after bulk data") // Skip trailing newline
        if (is.read() != 10) throw new RedisProtocolException("Expected newline after bulk data")
        arr
      }.get
    }

    def close() = stream foreach (_.close())
    def clear() = stream = None
  }

  class StreamWriter extends RedisStreamWriter {
    var stream: Option[OutputStream] = None

    def write(data: Array[Byte]) {
      if(!connected) connect;
      stream foreach {os =>
        os.write(data)
        os.flush
      }
    }

    def close() = stream foreach (_.close())
    def clear() = stream = None
  }

}

trait RedisStreamReader {
  def read[T](handler: Reply[T]): T
  def readBulk(count: Int): Array[Byte]
}

trait RedisStreamWriter {
  def write(data: Array[Byte]): Unit
}
