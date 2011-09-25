/**
 * Copyright (C) 2009-2011 Scalable Solutions AB <http://scalablesolutions.se>
 */
package akka.actor

import akka.util.ByteString
import akka.dispatch.MessageInvocation
import akka.event.EventHandler

import java.net.InetSocketAddress
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference
import java.nio.ByteBuffer
import java.nio.channels.{
  SelectableChannel,
  ReadableByteChannel,
  WritableByteChannel,
  SocketChannel,
  ServerSocketChannel,
  Selector,
  SelectionKey,
  CancelledKeyException
}

import scala.collection.mutable
import scala.collection.immutable.Queue
import scala.annotation.tailrec
import scala.util.continuations._

import com.eaio.uuid.UUID

object IO {

  sealed trait Handle {
    this: Product ⇒
    def owner: ActorRef
    def ioManager: ActorRef
    def uuid: UUID
    override lazy val hashCode = scala.runtime.ScalaRunTime._hashCode(this)

    def asReadable: ReadHandle = sys error "Not readable"
    def asWritable: WriteHandle = sys error "Not writable"
    def asSocket: SocketHandle = sys error "Not a socket"
    def asServer: ServerHandle = sys error "Not a server"

    def close(): Unit = ioManager ! Close(this)
  }

  sealed trait ReadHandle extends Handle with Product {
    override def asReadable = this
  }

  sealed trait WriteHandle extends Handle with Product {
    override def asWritable = this

    def write(bytes: ByteString): Unit = ioManager ! Write(this, bytes)
  }

  case class SocketHandle(owner: ActorRef, ioManager: ActorRef, uuid: UUID = new UUID()) extends ReadHandle with WriteHandle {
    override def asSocket = this
  }

  case class ServerHandle(owner: ActorRef, ioManager: ActorRef, uuid: UUID = new UUID()) extends Handle {
    override def asServer = this

    def accept(socketOwner: ActorRef): SocketHandle = {
      val socket = SocketHandle(socketOwner, ioManager)
      ioManager ! Accept(socket, this)
      socket
    }

    def accept()(implicit socketOwner: ScalaActorRef): SocketHandle = accept(socketOwner)
  }

  sealed trait IOMessage
  case class Listen(server: ServerHandle, address: InetSocketAddress) extends IOMessage
  case class NewClient(server: ServerHandle) extends IOMessage
  case class Accept(socket: SocketHandle, server: ServerHandle) extends IOMessage
  case class Connect(socket: SocketHandle, address: InetSocketAddress) extends IOMessage
  case class Connected(socket: SocketHandle) extends IOMessage
  case class Close(handle: Handle) extends IOMessage
  case class Closed(handle: Handle, cause: Option[Exception]) extends IOMessage
  case class Read(handle: ReadHandle, bytes: ByteString) extends IOMessage
  case class Write(handle: WriteHandle, bytes: ByteString) extends IOMessage

  def listen(ioManager: ActorRef, address: InetSocketAddress, owner: ActorRef): ServerHandle = {
    val server = ServerHandle(owner, ioManager)
    ioManager ! Listen(server, address)
    server
  }

  def listen(ioManager: ActorRef, address: InetSocketAddress)(implicit sender: ScalaActorRef): ServerHandle =
    listen(ioManager, address, sender)

  def listen(ioManager: ActorRef, host: String, port: Int, owner: ActorRef): ServerHandle =
    listen(ioManager, new InetSocketAddress(host, port), owner)

  def listen(ioManager: ActorRef, host: String, port: Int)(implicit sender: ScalaActorRef): ServerHandle =
    listen(ioManager, new InetSocketAddress(host, port), sender)

  def connect(ioManager: ActorRef, address: InetSocketAddress, owner: ActorRef): SocketHandle = {
    val socket = SocketHandle(owner, ioManager)
    ioManager ! Connect(socket, address)
    socket
  }

  def connect(ioManager: ActorRef, address: InetSocketAddress)(implicit sender: ScalaActorRef): SocketHandle =
    connect(ioManager, address, sender)

  def connect(ioManager: ActorRef, host: String, port: Int, owner: ActorRef): SocketHandle =
    connect(ioManager, new InetSocketAddress(host, port), owner)

  def connect(ioManager: ActorRef, host: String, port: Int)(implicit sender: ScalaActorRef): SocketHandle =
    connect(ioManager, new InetSocketAddress(host, port), sender)

  object Iteratee {
    def apply[A](value: A): Iteratee[A] = Done(value)
    val unit: Iteratee[Unit] = Done(())
  }

  sealed abstract class Iteratee[+A] {

    final def apply(bytes: ByteString): Iteratee[A] = this match {
      case _ if bytes.isEmpty ⇒ this
      case Done(value, rest)  ⇒ Done(value, rest ++ bytes)
      case Cont(f)            ⇒ f(bytes)
      case Failure(e, rest)   ⇒ Failure(e, rest ++ bytes)
    }

    final def get: A = this match {
      case Done(value, _) ⇒ value
      case Cont(_)        ⇒ sys.error("Incomplete Iteratee")
      case Failure(e, _)  ⇒ throw e
    }

    final def flatMap[B](f: A ⇒ Iteratee[B]): Iteratee[B] = this match {
      case Done(value, rest)      ⇒ f(value)(rest)
      case Cont(k: Cont.Chain[_]) ⇒ Cont(k :+ f)
      case Cont(k)                ⇒ Cont(Cont.Chain(k, f))
      case failure: Failure       ⇒ failure
    }

    final def map[B](f: A ⇒ B): Iteratee[B] = this match {
      case Done(value, rest)      ⇒ Done(f(value), rest)
      case Cont(k: Cont.Chain[_]) ⇒ Cont(k :+ ((a: A) ⇒ Done(f(a))))
      case Cont(k)                ⇒ Cont(Cont.Chain(k, (a: A) ⇒ Done(f(a))))
      case failure: Failure       ⇒ failure
    }

  }

  final case class Done[+A](result: A, remaining: ByteString = ByteString.empty) extends Iteratee[A]

  object Cont {
    private[akka] object Chain {
      def apply[A](f: ByteString ⇒ Iteratee[A]) = new Chain[A](f, Queue.empty)
      def apply[A, B](f: ByteString ⇒ Iteratee[A], k: A ⇒ Iteratee[B]) = new Chain[B](f, Queue(k.asInstanceOf[Any ⇒ Iteratee[Any]]))
    }

    private[akka] final class Chain[A] private (cur: ByteString ⇒ Iteratee[Any], queue: Queue[Any ⇒ Iteratee[Any]]) extends (ByteString ⇒ Iteratee[A]) {

      def :+[B](f: A ⇒ Iteratee[B]) = new Chain[B](cur, queue enqueue f.asInstanceOf[Any ⇒ Iteratee[Any]])

      def apply(input: ByteString): Iteratee[A] = {
        @scala.annotation.tailrec
        def run(prev: Any, rest: ByteString, queue: Queue[Any ⇒ Iteratee[Any]]): Iteratee[A] = {
          if (queue.isEmpty) Done[A](prev.asInstanceOf[A], rest)
          else {
            val (head, tail) = queue.dequeue
            head.apply(prev)(rest) match {
              case Done(result, more) ⇒ run(result, more, tail)
              case Cont(k)            ⇒ Cont(new Chain(k, tail))
              case failure: Failure   ⇒ failure
            }
          }
        }
        cur(input) match {
          case Done(result, rest) ⇒ run(result, rest, queue)
          case Cont(k)            ⇒ Cont(new Chain(k, queue))
          case failure: Failure   ⇒ failure
        }
      }
    }

  }
  final case class Cont[+A](f: ByteString ⇒ Iteratee[A]) extends Iteratee[A]

  final case class Failure(exception: Throwable, remaining: ByteString = ByteString.empty) extends Iteratee[Nothing]

  /**
   * A mutable reference to an Iteratee. Not thread safe.
   */
  final class IterateeRef[A](initial: Iteratee[A]) {

    private var _value = initial

    def flatMap(f: A ⇒ Iteratee[A]): Unit = _value = _value flatMap f

    def map(f: A ⇒ A): Unit = _value = _value map f

    def apply(bytes: ByteString): Unit = _value = _value(bytes)

  }

  def takeUntil(delimiter: ByteString, inclusive: Boolean = false): Iteratee[ByteString] = {
    def step(bytes: ByteString, start: Int): Iteratee[ByteString] = {
      val idx = bytes.indexOfSlice(delimiter, start)
      if (idx >= 0) {
        val index = if (inclusive) idx + delimiter.length else idx
        Done(bytes take index, bytes drop (index + delimiter.length))
      } else {
        Cont(more ⇒ step(bytes ++ more, math.max(bytes.length - delimiter.length, 0)))
      }
    }

    Cont(step(_, 0))
  }

  def take(length: Int): Iteratee[ByteString] = {
    def step(bytes: ByteString): Iteratee[ByteString] =
      if (bytes.length >= length)
        Done(bytes.take(length), bytes.drop(length))
      else
        Cont(more ⇒ step(bytes ++ more))

    Cont(step)
  }

  val takeAll: Iteratee[ByteString] = Cont { bytes ⇒
    if (bytes.nonEmpty) Done(bytes) else takeAll
  }

}

class IOManager(bufferSize: Int = 8192) extends Actor {
  import SelectionKey.{ OP_READ, OP_WRITE, OP_ACCEPT, OP_CONNECT }
  import IOWorker._

  var worker: IOWorker = _

  override def preStart: Unit = {
    worker = new IOWorker(self, bufferSize)
    worker.start
  }

  def receive = {
    case IO.Listen(server, address) ⇒
      val channel = ServerSocketChannel open ()
      channel configureBlocking false
      channel.socket bind address
      worker(Register(server, channel, OP_ACCEPT))

    case IO.Connect(socket, address) ⇒
      val channel = SocketChannel open ()
      channel configureBlocking false
      channel connect address
      worker(Register(socket, channel, OP_CONNECT | OP_READ))

    case IO.Accept(socket, server) ⇒ worker(Accepted(socket, server))
    case IO.Write(handle, data)    ⇒ worker(Write(handle, data.asByteBuffer))
    case IO.Close(handle)          ⇒ worker(Close(handle))
  }

  override def postStop: Unit = {
    worker(Shutdown)
  }

}

private[akka] object IOWorker {
  sealed trait Request
  case class Register(handle: IO.Handle, channel: SelectableChannel, ops: Int) extends Request
  case class Accepted(socket: IO.SocketHandle, server: IO.ServerHandle) extends Request
  case class Write(handle: IO.WriteHandle, data: ByteBuffer) extends Request
  case class Close(handle: IO.Handle) extends Request
  case object Shutdown extends Request
}

private[akka] class IOWorker(ioManager: ActorRef, val bufferSize: Int) {
  import SelectionKey.{ OP_READ, OP_WRITE, OP_ACCEPT, OP_CONNECT }
  import IOWorker._

  type ReadChannel = ReadableByteChannel with SelectableChannel
  type WriteChannel = WritableByteChannel with SelectableChannel

  implicit val optionIOManager: Some[ActorRef] = Some(ioManager)

  def apply(request: Request): Unit =
    addRequest(request)

  def start(): Unit =
    thread.start

  // private

  private val selector: Selector = Selector open ()

  private val _requests = new AtomicReference(List.empty[Request])

  private var accepted = Map.empty[IO.ServerHandle, Queue[SelectableChannel]].withDefaultValue(Queue.empty)

  private var channels = Map.empty[IO.Handle, SelectableChannel]

  private var writes = Map.empty[IO.WriteHandle, Queue[ByteBuffer]].withDefaultValue(Queue.empty)

  private val buffer = ByteBuffer.allocate(bufferSize)

  private val thread = new Thread("io-worker") {
    override def run(): Unit = {
      while (selector.isOpen) {
        selector select ()
        val keys = selector.selectedKeys.iterator
        while (keys.hasNext) {
          val key = keys next ()
          keys remove ()
          if (key.isValid) { process(key) }
        }
        _requests.getAndSet(Nil).reverse foreach {
          case Register(handle, channel, ops) ⇒
            channels += (handle -> channel)
            channel register (selector, ops, handle)
          case Accepted(socket, server) ⇒
            val (channel, rest) = accepted(server).dequeue
            if (rest.isEmpty) accepted -= server
            else accepted += (server -> rest)
            channels += (socket -> channel)
            channel register (selector, OP_READ, socket)
          case Write(handle, data) ⇒
            if (channels contains handle) {
              val queue = writes(handle)
              if (queue.isEmpty) addOps(handle, OP_WRITE)
              writes += (handle -> queue.enqueue(data))
            }
          case Close(handle) ⇒
            cleanup(handle, None)
          case Shutdown ⇒
            channels.values foreach (_.close)
            selector.close
        }
      }
    }
  }

  private def process(key: SelectionKey): Unit = {
    val handle = key.attachment.asInstanceOf[IO.Handle]
    try {
      if (key.isConnectable) key.channel match {
        case channel: SocketChannel ⇒ connect(handle.asSocket, channel)
      }
      if (key.isAcceptable) key.channel match {
        case channel: ServerSocketChannel ⇒ accept(handle.asServer, channel)
      }
      if (key.isReadable) key.channel match {
        case channel: ReadChannel ⇒ read(handle.asReadable, channel)
      }
      if (key.isWritable) key.channel match {
        case channel: WriteChannel ⇒
          try {
            write(handle.asWritable, channel)
          } catch {
            case e: IOException ⇒
            // ignore, let it fail on read to ensure nothing left in read buffer.
          }
      }
    } catch {
      case e: CancelledKeyException        ⇒ cleanup(handle, Some(e))
      case e: IOException                  ⇒ cleanup(handle, Some(e))
      case e: ActorInitializationException ⇒ cleanup(handle, Some(e))
    }
  }

  private def cleanup(handle: IO.Handle, cause: Option[Exception]): Unit = {
    handle match {
      case server: IO.ServerHandle  ⇒ accepted -= server
      case writable: IO.WriteHandle ⇒ writes -= writable
    }
    channels.get(handle) match {
      case Some(channel) ⇒
        channel.close
        channels -= handle
        try {
          handle.owner ! IO.Closed(handle, cause)
        } catch {
          case e: ActorInitializationException ⇒
            EventHandler debug (this, "IO.Handle's owner not running")
        }
      case None ⇒
    }
  }

  private def setOps(handle: IO.Handle, ops: Int): Unit =
    channels(handle) keyFor selector interestOps ops

  private def addOps(handle: IO.Handle, ops: Int): Unit = {
    val key = channels(handle) keyFor selector
    val cur = key.interestOps
    key interestOps (cur | ops)
  }

  private def removeOps(handle: IO.Handle, ops: Int): Unit = {
    val key = channels(handle) keyFor selector
    val cur = key.interestOps
    key interestOps (cur - (cur & ops))
  }

  private def connect(socket: IO.SocketHandle, channel: SocketChannel): Unit = {
    if (channel.finishConnect) {
      removeOps(socket, OP_CONNECT)
      socket.owner ! IO.Connected(socket)
    } else {
      cleanup(socket, None) // TODO: Add a cause
    }
  }

  @tailrec
  private def accept(server: IO.ServerHandle, channel: ServerSocketChannel): Unit = {
    val socket = channel.accept
    if (socket ne null) {
      socket configureBlocking false
      accepted += (server -> (accepted(server) enqueue socket))
      server.owner ! IO.NewClient(server)
      accept(server, channel)
    }
  }

  @tailrec
  private def read(handle: IO.ReadHandle, channel: ReadChannel): Unit = {
    buffer.clear
    val readLen = channel read buffer
    if (readLen == -1) {
      cleanup(handle, None) // TODO: Add a cause
    } else if (readLen > 0) {
      buffer.flip
      handle.owner ! IO.Read(handle, ByteString(buffer))
      if (readLen == buffer.capacity) read(handle, channel)
    }
  }

  @tailrec
  private def write(handle: IO.WriteHandle, channel: WriteChannel): Unit = {
    val queue = writes(handle)
    if (queue.nonEmpty) {
      val (buf, bufs) = queue.dequeue
      val writeLen = channel write buf
      if (buf.remaining == 0) {
        if (bufs.isEmpty) {
          writes -= handle
          removeOps(handle, OP_WRITE)
        } else {
          writes += (handle -> bufs)
          write(handle, channel)
        }
      }
    }
  }

  @tailrec
  private def addRequest(req: Request): Unit = {
    val requests = _requests.get
    if (_requests compareAndSet (requests, req :: requests))
      selector wakeup ()
    else
      addRequest(req)
  }
}
