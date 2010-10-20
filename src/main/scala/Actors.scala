package net.fyrie
package redis
package actors

import messages._

import handlers.{ Handler }

import se.scalablesolutions.akka.actor.{ Actor, ActorRef }
import Actor.{ actorOf }
import se.scalablesolutions.akka.config.Config._

import se.scalablesolutions.akka.dispatch._

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
  val bufferSize = config.getInt("fyrie-redis.buffer-size", 8192)
  val bufferDirect = config.getBool("fyrie-redis.buffer-direct", false)

  var channel: SocketChannel = _

  var writeSource: DispatchSource = _
  var readSource: DispatchSource = _

  var closed = false

  val writeQueue = new mutable.Queue[ByteBuffer]
  val handlerQueue = new mutable.Queue[(Handler[_,_], Option[CompletableFuture[Any]])]

  var readBufOverflowStream = Stream.continually {
    val b = if (bufferDirect) ByteBuffer.allocateDirect(bufferSize) else ByteBuffer.allocate(bufferSize)
    b.flip
    b
  }
  var readBufStream = readBufOverflowStream
  def readBuf = readBufStream.head

  var statLog: StatLog = NoStatLog

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
    statLog.log("buffer-compact")(readBuf.compact)
    statLog.log("read")(channel.read(readBuf)) match {
      case -1 => close
      case 0 if !readBuf.hasRemaining =>
        log.debug("IO: Buffer full")
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
    if (writeQueue.isEmpty) {
      writeSource.suspend
    } else {
      val count = statLog.log("write")(channel.write(writeQueue.take(20).toArray))
      log.debug("IO: wrote " + count)
      while (!(writeQueue.isEmpty || writeQueue.front.hasRemaining)) writeQueue.dequeue
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
      writeQueue += ByteBuffer.wrap(bytes)
      handlerQueue += ((handler, self.senderFuture))
      if (writeSource.isSuspended) {
        writeSource.resume
      } else (if (writeQueue.length > 20) write)
    case newStatLog: StatLog =>
      statLog = newStatLog
  }

  def processResponse(data: Array[Byte]) {
    val (handler, future) = handlerQueue.dequeue
    handler(data, future).reverse.foreach(_ +=: handlerQueue)
  }

  def notFoundResponse {
    val (handler, future) = handlerQueue.dequeue
    future foreach (_.completeWithResult(None))
  }

  def errorResponse(data: Array[Byte]) {
    val (handler, future) = handlerQueue.dequeue
    future foreach (_.completeWithException(new RedisErrorException(new String(data, "UTF-8"))))
  }

  abstract class ReadHandler {
    def apply: Boolean
  }

  var readHandler: ReadHandler = Idle

  object Idle extends ReadHandler {
    def apply = {
      if (readBuf.remaining > 0) {
        statLog.log("read-reply-marker") {
          readHandler = readBuf.get.toChar match {
            case '+' => ReadString
            case '-' => ReadError
            case ':' => ReadString
            case '$' => ReadBulk
            case '*' => ReadMultiBulk
            case x => error("Invalid Byte: " + x.toByte)
          }
        }
        true
      } else false
    }
  }

  val EOL = Array(13.toByte, 10.toByte)

  def readSingleLine: Option[Array[Byte]] = {
    statLog.log("read-single-line") {
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
  }

  object ReadString extends ReadHandler {
    def apply = {
      readSingleLine map { (data) =>
        statLog.log("process-string")(processResponse(data))
        readHandler = Idle
        true
      } getOrElse false
    }
  }

  object ReadError extends ReadHandler {
    def apply = {
      readSingleLine map { (data) =>
        errorResponse(data)
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
          notFoundResponse
          Idle
        }
        true
      } getOrElse false
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
        statLog.log("process-bulk") {
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
          processResponse(data)
        }
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
        statLog.log("process-multibulk")(processResponse(data))
        readHandler = Idle
        true
      } getOrElse false
    }
  }

}

trait StatLog {
  def log[T](name: String)(f: => T): T
}

object NoStatLog extends StatLog {
  def log[T](name: String)(f: => T): T = f
}

class StatLogToActor(actor: ActorRef) extends StatLog {
  def log[T](name: String)(f: => T): T = {
    val startTime = System.nanoTime
    val result = f
    val endTime = System.nanoTime
    actor ! (name, startTime, endTime)
    result
  }
}

class StatLogActor extends Actor {
  self.dispatcher = Dispatchers.globalHawtDispatcher

  var timeSplit = Map.empty[String, Double].withDefaultValue(0.0)

  def receive = {
    case (name: String, startTime: Long, endTime: Long) =>
      timeSplit += name -> (timeSplit(name) + ((endTime - startTime).toDouble / 1000000.0))
    case 'printTimeSplit =>
      val total = timeSplit.values.sum
      println(timeSplit.map { case (k, v) => (k, (100 * v / total).toInt) })
    case 'resetStats =>
      timeSplit = timeSplit.empty
  }
}
