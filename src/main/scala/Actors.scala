package net.fyrie
package redis
package actors

import messages._

import handlers.{Handler}

import se.scalablesolutions.akka.actor.{Actor, ActorRef, FSM}
import Actor.{actorOf}
import se.scalablesolutions.akka.config.{AllForOneStrategy}
import se.scalablesolutions.akka.config.ScalaConfig.{LifeCycle, Permanent}

import se.scalablesolutions.akka.dispatch._

import org.fusesource.hawtdispatch._
import org.fusesource.hawtdispatch.ScalaDispatch._

import java.nio.channels.{SocketChannel, SelectionKey}
import java.nio.ByteBuffer
import java.io.IOException
import java.net.InetSocketAddress

import scala.collection.mutable
import scala.collection.immutable

class RedisClientSession(host: String = "localhost", port: Int = 6379, bufferSize: Int = 64*1024) extends Actor {
  self.dispatcher = Dispatchers.globalHawtDispatcher

  var channel: SocketChannel = _

  var writeSource: DispatchSource = _
  var readSource: DispatchSource = _

  var closed = false

  val writeQueue = new mutable.Queue[ByteBuffer]
  val handlerQueue = new mutable.Queue[(Handler[_], Option[CompletableFuture[Any]])]
  val sharedReadBuf = ByteBuffer.allocate(bufferSize)
  sharedReadBuf.limit(0)
  var readBuf = sharedReadBuf
  val overflow = new mutable.Queue[Array[Byte]]

  override def preStart = {
    channel = SocketChannel.open()
    channel.configureBlocking(false)
    channel.connect(new InetSocketAddress(host, port))

    writeSource = createSource(channel, SelectionKey.OP_WRITE, HawtDispatcher.queue(self))
    writeSource.setEventHandler(^{ write })
    writeSource.setCancelHandler(^{ close })

    readSource = createSource(channel, SelectionKey.OP_READ, HawtDispatcher.queue(self))
    readSource.setEventHandler(^{ read })
    readSource.setCancelHandler(^{ close })

    if (channel.isConnectionPending) channel.finishConnect
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
          log.debug("IO: Buffer full")
          readBuf.rewind
          val ar = new Array[Byte](readBuf.remaining - 1)
          readBuf.get(ar)
          overflow += ar
        case 0 => Unit
        case count: Int =>
          log.debug("IO: read "+count)
          readBuf.flip
          readHandler.apply
      }
  }

  def write(): Unit = catchio {
    if (writeQueue.isEmpty) {
      writeSource.suspend
    } else {
      log.debug("IO: writing")
      channel.write(writeQueue.toArray)
      while (!(writeQueue.isEmpty || writeQueue.front.hasRemaining)) (writeQueue.dequeue)
    }
  }

  def close() = {
    if( !closed ) {
      closed = true
      println("CLOSED")
    }
  }

  def receive = {
    case Request(bytes, handler) =>
      writeQueue += ByteBuffer.wrap(bytes)
      handlerQueue += ((handler, self.senderFuture))
      readSource.resume
      writeSource.resume
      write
  }

  def processResponse(data: Array[Byte]) {
    val (handler, future) = handlerQueue.dequeue
    handler(data, future).reverse.foreach( _ +=: handlerQueue)
  }

  def notFoundResponse {
    val (handler, future) = handlerQueue.dequeue
    future foreach (_.completeWithResult(Result(None)))
  }

  def errorResponse(data: Array[Byte]) {
    val (handler, future) = handlerQueue.dequeue
    future foreach (_.completeWithResult(Error(new RedisErrorException(new String(data, "UTF-8")))))
  }

  abstract class ReadHandler {
    def apply: Unit
  }

  var readHandler: ReadHandler = Idle

  object Idle extends ReadHandler {
    def apply {
      if (readBuf.remaining > 0) {
        readHandler = readBuf.get.toChar match {
          case '+' => ReadString
          case '-' => ReadError
          case ':' => ReadString
          case '$' => ReadBulk
          case '*' => ReadMultiBulk
          case x => error("Invalid Byte: "+x.toByte)
        }
        readHandler.apply
      }
    }
  }

  val EOL = Array(13.toByte, 10.toByte)

  def readSingleLine: Option[Array[Byte]] = {
    if (readBuf.remaining < 2) None else
      (0 to (readBuf.remaining - 2)) find (n =>
        readBuf.get(readBuf.position + n) == EOL(0) &&
        readBuf.get(readBuf.position + n + 1) == EOL(1)) map { n =>
          val ar = new Array[Byte](overflow.foldLeft(n)(_ + _.length))
          var pos = 0
          while (!overflow.isEmpty) {
            val o = overflow.dequeue
            Array.copy(o, 0, ar, pos, o.length)
            pos += o.length
          }
          if (ar.length - pos > 0)
            readBuf.get(ar, pos, ar.length - pos)
          readBuf.position(readBuf.position + 2) // skip EOL
          ar
        }
  }

  object ReadString extends ReadHandler {
    def apply {
      readSingleLine foreach { (data) =>
        processResponse(data)
        readHandler = Idle
        readHandler.apply
      }
    }
  }

  object ReadError extends ReadHandler {
    def apply {
      readSingleLine foreach { (data) =>
        errorResponse(data)
        readHandler = Idle
        readHandler.apply
      }
    }
  }

  object ReadBulk extends ReadHandler {
    def apply {
      readSingleLine foreach { (data) =>
        val size = new String(data, "UTF-8").toInt
        readHandler = if (size > -1) (new ReadBulkData(size)) else {
          notFoundResponse
          Idle
        }
        readHandler.apply
      }
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
      readBuf = buf
    }
    
    def apply {
      if (size <= readBuf.remaining) {
        val data = if (tempBuffer) {
          log.debug("IO: Releasing temporary buffer")
          val ar = readBuf.array
          readBuf = sharedReadBuf
          ar
        } else {
          val ar = new Array[Byte](size)
          readBuf.get(ar)
          ar
        }
        processResponse(data)
        readHandler = ReadEOL
        readHandler.apply
      }
    }
  }

  object ReadEOL extends ReadHandler {
    def apply {
      if (readBuf.remaining >= 2) {
        if (readBuf.get(readBuf.position) == EOL(0) && readBuf.get(readBuf.position + 1) == EOL(1)) {
          readBuf.position(readBuf.position + 2)
          readHandler = Idle
          readHandler.apply
        }
      }
    }
  }

  object ReadMultiBulk extends ReadHandler {
    def apply {
      readSingleLine foreach { (data) =>
        processResponse(data)
        readHandler = Idle
        readHandler.apply
      }
    }
  }

}
