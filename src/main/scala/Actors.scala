package net.fyrie
package redis
package actors

import messages._
import handlers._
import RedisType._

import akka.actor.{ Actor, ActorRef }
import Actor.{ actorOf }
import akka.config.Config._

import akka.dispatch._

import org.fusesource.hawtdispatch._
import org.fusesource.hawtdispatch.ScalaDispatch._

import java.nio.channels.{ SocketChannel, SelectionKey }
import java.nio.ByteBuffer
import java.io.IOException
import java.net.InetSocketAddress

import scala.collection.mutable
import scala.collection.immutable

final class RedisClientSession(host: String, port: Int) extends Actor {

  self.dispatcher = Dispatchers.globalHawtDispatcher

  val hawtPin = config.getBool("fyrie-redis.hawt-pin", false)
  val readBufSize = config.getInt("fyrie-redis.read-buffer-size", 8192)
  val readBufDirect = config.getBool("fyrie-redis.read-buffer-direct", false)
  val writeBufSize = config.getInt("fyrie-redis.write-buffer-size", 8192)
  val writeBufDirect = config.getBool("fyrie-redis.write-buffer-direct", false)

  var channel: SocketChannel = _

  var writeSource: DispatchSource = _
  var readSource: DispatchSource = _

  var closed = false

  var writeBuf = (writeBufAllocate, writeBufAllocate)
  writeBuf._2.flip

  def writeBufAllocate: ByteBuffer = if (writeBufDirect) ByteBuffer.allocateDirect(writeBufSize) else ByteBuffer.allocate(writeBufSize)

  val writeQueue = new mutable.Queue[ByteBuffer]
  val responseQueue = new mutable.Queue[Responder]

  var readBufOverflowStream = Stream.continually {
    val b = if (readBufDirect) ByteBuffer.allocateDirect(readBufSize) else ByteBuffer.allocate(readBufSize)
    b.flip
    b
  }
  var readBufStream = readBufOverflowStream
  def readBuf = readBufStream.head

  override def preStart = {
    channel = SocketChannel.open()
    if (hawtPin)
      HawtDispatcher.pin(self)
    channel.configureBlocking(false)
    log.debug("Connecting to host " + host + " on port " + port)
    channel.connect(new InetSocketAddress(host, port))

    writeSource = createSource(channel, SelectionKey.OP_WRITE, HawtDispatcher.queue(self))
    writeSource.setEventHandler(^ { write })
    writeSource.setCancelHandler(^ { close })

    readSource = createSource(channel, SelectionKey.OP_READ, HawtDispatcher.queue(self))
    readSource.setEventHandler(^ { read })
    readSource.setCancelHandler(^ { close })

    if (channel.isConnectionPending) channel.finishConnect
    log.debug("Socket Receive Buffer: " + channel.socket.getReceiveBufferSize)
    log.debug("Socket Send Buffer: " + channel.socket.getSendBufferSize)
    readSource.resume
  }

  override def postStop = {
    closed = true
    readSource.release
    writeSource.release
    channel.close
  }

  def catchio(f: => Unit) {
    try {
      f
    } catch {
      case e: IOException => close
    }
  }

  def read(): Unit = catchio {
    readBuf.compact
    channel.read(readBuf) match {
      case -1 => close
      case 0 if !readBuf.hasRemaining =>
        log.debug("IO: Read buffer full")
        val oldBuf = readBuf
        readBufStream = readBufStream.tail
        if (oldBuf.get(oldBuf.limit - 1) == EOL(0)) { // Don't separate an EOL
          readBuf.compact
          readBuf.put(EOL(0))
          readBuf.flip
          oldBuf.limit(oldBuf.limit - 1)
        }
      case 0 => Unit
      case count: Int =>
        log.debug("IO: read " + count)
        readBuf.flip
        while (readHandler.apply) ()
    }
  }

  def write(): Unit = catchio {
    if (!writeBuf._2.hasRemaining) {
      writeBufFill()
      writeBufFlip()
      if (writeBuf._2.hasRemaining) {
        write()
      } else {
        writeSource.suspend
      }
    } else {
      val count = channel.write(writeBuf._2)
      log.debug("IO: wrote " + count)
    }
  }

  def close() = {
    if (!closed) {
      closed = true
      log.debug("CLOSED")
    }
  }

  def receive = {
    case Request(bytes, handler) =>
      responseQueue += Responder(handler, self.sender, self.senderFuture)
      writeQueue += ByteBuffer.wrap(bytes)
      if (writeSource.isSuspended) writeSource.resume
      writeBufFill()
  }

  def writeBufFill() {
    while (writeBuf._1.hasRemaining && !writeQueue.isEmpty) {
      val bb = writeQueue.dequeue
      if (writeBuf._1.remaining >= bb.remaining) {
        writeBuf._1.put(bb)
      } else {
        bb.limit(bb.position + writeBuf._1.remaining)
        writeBuf._1.put(bb)
        bb.limit(bb.capacity)
        bb +=: writeQueue
        log.debug("IO: Filled waiting write buffer")
      }
    }
  }

  def writeBufFlip() {
    writeBuf = writeBuf.swap
    writeBuf._1.clear
    writeBuf._2.flip
  }

  def respond[T: RedisType: Manifest](data: T) {
    responseQueue.dequeue.apply(data).reverse.foreach(_ +=: responseQueue)
  }

  abstract class ReadHandler {
    def apply: Boolean
  }

  var readHandler: ReadHandler = Idle

  object Idle extends ReadHandler {
    def apply = {
      if (readBuf.remaining > 0) {
        readHandler = readBuf.get.toChar match {
          case '+' => ReadString
          case '-' => ReadError
          case ':' => ReadInteger
          case '$' => ReadBulk
          case '*' => ReadMultiBulk
          case x => error("Invalid Byte: " + x.toByte)
        }
        true
      } else false
    }
  }

  val EOL = Array(13.toByte, 10.toByte)

  def readSingleLine: Option[Array[Byte]] = {
    if (readBuf.remaining < 2) None else
      (0 to (readBuf.remaining - 2)) find (n =>
        readBuf.get(readBuf.position + n) == EOL(0) &&
          readBuf.get(readBuf.position + n + 1) == EOL(1)) map { n =>
        val overflow = readBufOverflowStream.takeWhile(!_.hasRemaining)
        if (!overflow.isEmpty) log.debug("IO: Draining " + overflow.length + " overflow buffers")
        val ar = new Array[Byte](overflow.foldLeft(n)(_ + _.limit))
        var pos = 0
        overflow foreach { o =>
          o.flip
          o.get(ar, pos, o.limit)
          pos += o.limit
        }
        readBufOverflowStream = readBufStream
        if (ar.length - pos > 0)
          readBuf.get(ar, pos, ar.length - pos)
        readBuf.position(readBuf.position + 2) // skip EOL
        ar
      }
  }

  object ReadString extends ReadHandler {
    def apply = {
      readSingleLine map { (data) =>
        respond(RedisString(data))
        readHandler = Idle
      } isDefined
    }
  }

  object ReadInteger extends ReadHandler {
    def apply = {
      readSingleLine map { (data) =>
        respond(RedisInteger(data))
        readHandler = Idle
      } isDefined
    }
  }

  object ReadError extends ReadHandler {
    def apply = {
      readSingleLine map { (data) =>
        respond(RedisError(data))
        readHandler = Idle
        true
      } getOrElse false
    }
  }

  object ReadBulk extends ReadHandler {
    def apply = {
      readSingleLine map { (data) =>
        val size = new String(data, "UTF-8").toInt
        readHandler = if (size > -1) (new ReadBulkData(size)) else {
          respond(Option.empty[Array[Byte]])
          Idle
        }
      } isDefined
    }
  }

  class ReadBulkData(val size: Int) extends ReadHandler {

    val tempBuffer = size > readBuf.capacity

    if (tempBuffer) {
      log.debug("IO: Buffer too small, creating a temporary buffer for data")
      val buf = ByteBuffer.allocate(size)
      val ar = buf.array
      buf.limit(readBuf.remaining)
      readBuf.get(ar, 0, readBuf.remaining)
      readBuf.limit(0)
      readBufStream = buf +: readBufStream
    }

    def apply = {
      if (size <= readBuf.remaining) {
        val data = if (tempBuffer) {
          log.debug("IO: Releasing temporary buffer")
          val ar = readBuf.array
          readBufStream = readBufStream.tail
          ar
        } else {
          val ar = new Array[Byte](size)
          readBuf.get(ar)
          ar
        }
        respond(RedisBulk(data))
        readHandler = ReadEOL
        true
      } else false
    }
  }

  object ReadEOL extends ReadHandler {
    def apply = {
      if (readBuf.remaining >= 2) {
        if (readBuf.get(readBuf.position) == EOL(0) && readBuf.get(readBuf.position + 1) == EOL(1)) {
          readBuf.position(readBuf.position + 2)
          readHandler = Idle
          true
        } else false
      } else false
    }
  }

  object ReadMultiBulk extends ReadHandler {
    def apply = {
      readSingleLine map { (data) =>
        respond(RedisMulti(data))
        readHandler = Idle
      } isDefined
    }
  }

}

