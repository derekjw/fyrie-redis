package net.fyrie
package redis
package akka
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

  val writeQueue = new mutable.Queue[Array[Byte]]
  var writeBuf: Option[ByteBuffer] = None
  val readQueue = new mutable.Queue[Handler[_]]
  val sharedReadBuf = ByteBuffer.allocate(bufferSize) 
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
      channel.read(readBuf) match {
        case -1 => close
        case 0 if !readBuf.hasRemaining =>
          log.debug("IO: Buffer full")
          readBuf.rewind
          val ar = new Array[Byte](readBuf.remaining - 1)
          readBuf.get(ar)
          readBuf.compact
          overflow += ar
        case 0 => Unit
        case count: Int =>
          log.debug("IO: read "+count)
          readHandler(count)
      }
  }

  def write(): Unit = catchio {
      writeBuf match {
        case Some(buf) =>
          log.debug("IO: writing")
          channel.write(buf)
          if (!buf.hasRemaining)
            log.debug("IO: writing done")
            writeBuf = None
        case None if writeQueue.isEmpty =>
          writeSource.suspend
        case None =>
          writeBuf = Some(ByteBuffer.wrap(writeQueue.dequeue))
          write
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
      writeQueue += bytes
      readQueue += handler
      readSource.resume
      writeSource.resume
      write
  }

  abstract class ReadHandler extends Function1[Int, Unit]

  var readHandler: ReadHandler = Idle

  object Idle extends ReadHandler {
    def apply(count: Int) {
      if (count > 0) {
        val marker = readBuf.get(readBuf.position - count).toChar
        readHandler = marker match {
          case '+' => ReadString
          case '-' => ReadError
          case ':' => ReadInteger
          case '$' => ReadBulk
          case '*' => ReadMultiBulk
          case x => error("Invalid Byte: "+x.toByte)
        }
        readHandler(count - 1)
      }
    }
  }

  val EOL = Array(13.toByte, 10.toByte)

  def readSingleLine(count: Int): Option[String] =
    ((count).to(1, -1)) find (n =>
      readBuf.get(readBuf.position - n - 1) == EOL(0) &&
      readBuf.get(readBuf.position - n) == EOL(1)) map { n =>
        readBuf.limit(readBuf.position)
        val ar = new Array[Byte](overflow.foldLeft(readBuf.position - n - 2)(_ + _.length))
        var skippedFirst = false
        var pos = 0
        while (!overflow.isEmpty) {
          val o = overflow.dequeue
          Array.copy(o, if (skippedFirst) 0 else 1, ar, pos, o.length - (if (skippedFirst) 0 else 1))
          pos += o.length - (if (skippedFirst) 0 else 1)
          skippedFirst = true
        }
        readBuf.position(if (skippedFirst) 0 else 1)
        if (ar.length - pos > 0)
          readBuf.get(ar, pos, ar.length - pos)
        readBuf.position(readBuf.limit - n + 1)
        readBuf.compact
        new String(ar, "UTF-8")
      }

  object ReadString extends ReadHandler {
    def apply(count: Int) {
      readSingleLine(count) foreach { (string) =>
        println("Got String: "+string)
        readHandler = Idle
        readHandler(readBuf.position)
      }
    }
  }

  object ReadError extends ReadHandler {
    def apply(count: Int) {
      readSingleLine(count) foreach { (string) =>
        println("Got Error: "+string)
        readHandler = Idle
        readHandler(readBuf.position)
      }
    }
  }

  object ReadInteger extends ReadHandler {
    def apply(count: Int) {
      readSingleLine(count) foreach { (string) =>
        println("Got Integer: "+string.toLong)
        readHandler = Idle
        readHandler(readBuf.position)
      }
    }
  }

  object ReadBulk extends ReadHandler {
    def apply(count: Int) {
      readSingleLine(count) foreach { (string) =>
        val bulkSize = string.toInt
        println("Got Bulk: "+bulkSize)
        readHandler = new ReadBulkData(bulkSize)
        readHandler(readBuf.position)
      }
    }
  }

  class ReadBulkData(val size: Int) extends ReadHandler {

    val tempBuffer = size > readBuf.capacity
    
    if (tempBuffer) {
      log.debug("IO: Buffer too small, creating a temporary buffer for data")
      val buf = ByteBuffer.allocate(size)
      val ar = buf.array
      readBuf.flip
      val pos = readBuf.remaining
      readBuf.get(ar, 0, pos)
      readBuf.clear
      buf.position(pos)
      readBuf = buf
    }
    
    def apply(count: Int) {
      if (size <= readBuf.position) {
        val data = if (tempBuffer) {
          log.debug("IO: Releasing temporary buffer")
          val ar = readBuf.array
          readBuf = sharedReadBuf
          ar
        } else {
          val ar = new Array[Byte](size)
          readBuf.flip
          readBuf.get(ar)
          readBuf.compact
          ar
        }
        println("Got Bulk Data: " + new String(data, "UTF-8"))
        readHandler = ReadEOL
        readHandler(readBuf.position)
      }
    }
  }

  object ReadEOL extends ReadHandler {
    def apply(count: Int) {
      if (readBuf.get(0) == EOL(0) && readBuf.get(1) == EOL(1)) {
        readBuf.flip
        readBuf.position(2)
        readBuf.compact
        readHandler = Idle
        readHandler(readBuf.position)
      }
    }
  }

  object ReadMultiBulk extends ReadHandler {
    def apply(count: Int) {
      readSingleLine(count) foreach { (string) =>
        println("Got MultiBulk: "+string.toInt)
        readHandler = Idle
        readHandler(readBuf.position)
      }
    }
  }

}
