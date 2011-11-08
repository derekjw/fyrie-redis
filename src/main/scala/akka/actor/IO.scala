/**
 * Copyright (C) 2009-2011 Scalable Solutions AB <http://scalablesolutions.se>
 */
package akka.actor

import akka.util.ByteString
import akka.event.EventHandler
import java.net.InetSocketAddress
import java.io.IOException
import java.util.concurrent.atomic.{ AtomicReference, AtomicBoolean }
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
import scala.collection.generic.CanBuildFrom
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

  case class SocketHandle(owner: ActorRef, ioManager: ActorRef = IOManager.global, uuid: UUID = new UUID()) extends ReadHandle with WriteHandle {
    override def asSocket = this
  }

  case class ServerHandle(owner: ActorRef, ioManager: ActorRef = IOManager.global, uuid: UUID = new UUID()) extends Handle {
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

  def listen(ioManager: ActorRef, port: Int, owner: ActorRef): ServerHandle =
    listen(ioManager, new InetSocketAddress(port), owner)

  def listen(ioManager: ActorRef, port: Int)(implicit sender: ScalaActorRef): ServerHandle =
    listen(ioManager, new InetSocketAddress(port), sender)

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

  sealed trait Input {
    def ++(that: Input): Input
  }

  object Chunk {
    val empty = Chunk(ByteString.empty)
  }

  case class Chunk(bytes: ByteString) extends Input {
    def ++(that: Input) = that match {
      case Chunk(more) ⇒ Chunk(bytes ++ more)
      case _: EOF      ⇒ that
    }
  }

  case class EOF(cause: Option[Exception]) extends Input {
    def ++(that: Input) = this
  }

  object Iteratee {
    def apply[A](value: A): Iteratee[A] = Done(value)
    def apply(): Iteratee[Unit] = unit
    val unit: Iteratee[Unit] = Done(())
  }

  /**
   * A basic Iteratee implementation of Oleg's Iteratee (http://okmij.org/ftp/Streams.html).
   * No support for Enumerator or Input types other then ByteString at the moment.
   */
  sealed abstract class Iteratee[+A] {

    /**
     * Applies the given input to the Iteratee, returning the resulting Iteratee
     * and the unused Input.
     */
    final def apply(input: Input): (Iteratee[A], Input) = this match {
      case Cont(f) ⇒ f(input)
      case iter    ⇒ (iter, input)
    }

    final def get: A = this(EOF(None))._1 match {
      case Done(value) ⇒ value
      case Cont(_)     ⇒ sys.error("Divergent Iteratee")
      case Failure(e)  ⇒ throw e
    }

    final def flatMap[B](f: A ⇒ Iteratee[B]): Iteratee[B] = this match {
      case Done(value)       ⇒ f(value)
      case Cont(k: Chain[_]) ⇒ Cont(k :+ f)
      case Cont(k)           ⇒ Cont(Chain(k, f))
      case failure: Failure  ⇒ failure
    }

    final def map[B](f: A ⇒ B): Iteratee[B] = this match {
      case Done(value)       ⇒ Done(f(value))
      case Cont(k: Chain[_]) ⇒ Cont(k :+ ((a: A) ⇒ Done(f(a))))
      case Cont(k)           ⇒ Cont(Chain(k, (a: A) ⇒ Done(f(a))))
      case failure: Failure  ⇒ failure
    }

  }

  /**
   * An Iteratee representing a result and the remaining ByteString. Also used to
   * wrap any constants or precalculated values that need to be composed with
   * other Iteratees.
   */
  final case class Done[+A](result: A) extends Iteratee[A]

  /**
   * An Iteratee that still requires more input to calculate it's result.
   */
  final case class Cont[+A](f: Input ⇒ (Iteratee[A], Input)) extends Iteratee[A]

  /**
   * An Iteratee representing a failure to calcualte a result.
   * FIXME: move into 'Cont' as in Oleg's implementation
   */
  final case class Failure(exception: Throwable) extends Iteratee[Nothing]

  object IterateeRef {
    def sync[A](initial: Iteratee[A]): IterateeRefSync[A] = new IterateeRefSync(initial)
    def sync(): IterateeRefSync[Unit] = new IterateeRefSync(Iteratee.unit)

    def async[A](initial: Iteratee[A]): IterateeRefAsync[A] = new IterateeRefAsync(initial)
    def async(): IterateeRefAsync[Unit] = new IterateeRefAsync(Iteratee.unit)

    class Map[K, V] private (refFactory: ⇒ IterateeRef[V], underlying: mutable.Map[K, IterateeRef[V]] = mutable.Map.empty[K, IterateeRef[V]]) extends mutable.Map[K, IterateeRef[V]] {
      def get(key: K) = Some(underlying.getOrElseUpdate(key, refFactory))
      def iterator = underlying.iterator
      def +=(kv: (K, IterateeRef[V])) = { underlying += kv; this }
      def -=(key: K) = { underlying -= key; this }
      override def empty = new Map[K, V](refFactory)
    }
    object Map {
      def apply[K, V](refFactory: ⇒ IterateeRef[V]): IterateeRef.Map[K, V] = new Map(refFactory)
      def sync[K](): IterateeRef.Map[K, Unit] = new Map(IterateeRef.sync())
      def async[K](): IterateeRef.Map[K, Unit] = new Map(IterateeRef.async())
    }
  }

  /**
   * A mutable reference to an Iteratee. Not thread safe.
   *
   * Designed for use within an Actor.
   *
   * Includes mutable implementations of flatMap, map, and apply which
   * update the internal reference and return Unit.
   */
  trait IterateeRef[A] {
    def flatMap(f: A ⇒ Iteratee[A]): Unit
    def map(f: A ⇒ A): Unit
    def apply(input: Input): Unit
  }

  final class IterateeRefSync[A](initial: Iteratee[A]) extends IterateeRef[A] {
    private var _value: (Iteratee[A], Input) = (initial, Chunk.empty)
    def flatMap(f: A ⇒ Iteratee[A]): Unit = _value = _value match {
      case (iter, chunk @ Chunk(bytes)) if bytes.nonEmpty ⇒ (iter flatMap f)(chunk)
      case (iter, input)                                  ⇒ (iter flatMap f, input)
    }
    def map(f: A ⇒ A): Unit = _value = (_value._1 map f, _value._2)
    def apply(input: Input): Unit = _value = _value._1(_value._2 ++ input)
    def value: (Iteratee[A], Input) = _value
  }

  final class IterateeRefAsync[A](initial: Iteratee[A]) extends IterateeRef[A] {
    import akka.dispatch.Future
    private var _value: Future[(Iteratee[A], Input)] = Future((initial, Chunk.empty))
    def flatMap(f: A ⇒ Iteratee[A]): Unit = _value = _value map {
      case (iter, chunk @ Chunk(bytes)) if bytes.nonEmpty ⇒ (iter flatMap f)(chunk)
      case (iter, input)                                  ⇒ (iter flatMap f, input)
    }
    def map(f: A ⇒ A): Unit = _value = _value map (v ⇒ (v._1 map f, v._2))
    def apply(input: Input): Unit = _value = _value map (v ⇒ v._1(v._2 ++ input))
    def future: Future[(Iteratee[A], Input)] = _value
  }

  /**
   * An Iteratee that returns the ByteString prefix up until the supplied delimiter.
   * The delimiter is dropped by default, but it can be returned with the result by
   * setting 'inclusive' to be 'true'.
   */
  def takeUntil(delimiter: ByteString, inclusive: Boolean = false): Iteratee[ByteString] = {
    def step(taken: ByteString)(input: Input): (Iteratee[ByteString], Input) = input match {
      case Chunk(more) ⇒
        val bytes = taken ++ more
        val startIdx = bytes.indexOfSlice(delimiter, math.max(taken.length - delimiter.length, 0))
        if (startIdx >= 0) {
          val endIdx = startIdx + delimiter.length
          (Done(bytes take (if (inclusive) endIdx else startIdx)), Chunk(bytes drop endIdx))
        } else {
          (Cont(step(bytes)), Chunk.empty)
        }
      case eof ⇒ (Cont(step(taken)), eof)
    }

    Cont(step(ByteString.empty))
  }

  def takeWhile(p: (Byte) ⇒ Boolean): Iteratee[ByteString] = {
    def step(taken: ByteString)(input: Input): (Iteratee[ByteString], Input) = input match {
      case Chunk(more) ⇒
        val (found, rest) = more span p
        if (rest.isEmpty)
          (Cont(step(taken ++ found)), Chunk.empty)
        else
          (Done(taken ++ found), Chunk(rest))
      case eof ⇒ (Done(taken), eof)
    }

    Cont(step(ByteString.empty))
  }

  /**
   * An Iteratee that returns a ByteString of the requested length.
   */
  def take(length: Int): Iteratee[ByteString] = {
    def step(taken: ByteString)(input: Input): (Iteratee[ByteString], Input) = input match {
      case Chunk(more) ⇒
        val bytes = taken ++ more
        if (bytes.length >= length)
          (Done(bytes.take(length)), Chunk(bytes.drop(length)))
        else
          (Cont(step(bytes)), Chunk.empty)
      case eof ⇒ (Cont(step(taken)), eof)
    }

    Cont(step(ByteString.empty))
  }

  /**
   * An Iteratee that ignores the specified number of bytes.
   */
  def drop(length: Int): Iteratee[Unit] = {
    def step(left: Int)(input: Input): (Iteratee[Unit], Input) = input match {
      case Chunk(more) ⇒
        if (left > more.length)
          (Cont(step(left - more.length)), Chunk.empty)
        else
          (Done(), Chunk(more drop left))
      case eof ⇒ (Done(), eof)
    }

    Cont(step(length))
  }

  /**
   * An Iteratee that returns the remaining ByteString until an EOF is given.
   */
  val takeAll: Iteratee[ByteString] = {
    def step(taken: ByteString)(input: Input): (Iteratee[ByteString], Input) = input match {
      case Chunk(more) ⇒
        val bytes = taken ++ more
        (Cont(step(bytes)), Chunk.empty)
      case eof ⇒ (Done(taken), eof)
    }

    Cont(step(ByteString.empty))
  }

  /**
   * An Iteratee that returns any input it receives
   */
  val takeAny: Iteratee[ByteString] = Cont {
    case Chunk(bytes) if bytes.nonEmpty ⇒ (Done(bytes), Chunk.empty)
    case Chunk(bytes)                   ⇒ (takeAny, Chunk.empty)
    case eof                            ⇒ (Done(ByteString.empty), eof)
  }

  def takeList[A](length: Int)(iter: Iteratee[A]): Iteratee[List[A]] = {
    def step(left: Int, list: List[A]): Iteratee[List[A]] =
      if (left == 0) Done(list.reverse)
      else iter flatMap (a ⇒ step(left - 1, a :: list))

    step(length, Nil)
  }

  def peek(length: Int): Iteratee[ByteString] = {
    def step(taken: ByteString)(input: Input): (Iteratee[ByteString], Input) = input match {
      case Chunk(more) ⇒
        val bytes = taken ++ more
        if (bytes.length >= length)
          (Done(bytes.take(length)), Chunk(bytes))
        else
          (Cont(step(bytes)), Chunk.empty)
      case eof ⇒ (Cont(step(taken)), eof)
    }

    Cont(step(ByteString.empty))
  }

  def repeat(iter: Iteratee[Unit]): Iteratee[Unit] =
    iter flatMap (_ ⇒ repeat(iter))

  def traverse[A, B, M[A] <: Traversable[A]](in: M[A])(f: A ⇒ Iteratee[B])(implicit cbf: CanBuildFrom[M[A], B, M[B]]): Iteratee[M[B]] =
    fold(cbf(in), in)((b, a) ⇒ f(a) map (b += _)) map (_.result)

  def fold[A, B, M[A] <: Traversable[A]](initial: B, in: M[A])(f: (B, A) ⇒ Iteratee[B]): Iteratee[B] =
    (Iteratee(initial) /: in)((ib, a) ⇒ ib flatMap (b ⇒ f(b, a)))

  // private api

  private[akka] object Chain {
    def apply[A](f: Input ⇒ (Iteratee[A], Input)) = new Chain[A](f, Queue.empty)
    def apply[A, B](f: Input ⇒ (Iteratee[A], Input), k: A ⇒ Iteratee[B]) = new Chain[B](f, Queue(k.asInstanceOf[Any ⇒ Iteratee[Any]]))
  }

  /**
   * A function 'ByteString => Iteratee[A]' that composes with 'A => Iteratee[B]' functions
   * in a stack-friendly manner.
   *
   * For internal use within Iteratee.
   */
  private[akka] final case class Chain[A] private (cur: Input ⇒ (Iteratee[Any], Input), queue: Queue[Any ⇒ Iteratee[Any]]) extends (Input ⇒ (Iteratee[A], Input)) {

    def :+[B](f: A ⇒ Iteratee[B]) = new Chain[B](cur, queue enqueue f.asInstanceOf[Any ⇒ Iteratee[Any]])

    def apply(input: Input): (Iteratee[A], Input) = {
      @tailrec
      def run(result: (Iteratee[Any], Input), queue: Queue[Any ⇒ Iteratee[Any]]): (Iteratee[Any], Input) = {
        if (queue.isEmpty) result
        else result match {
          case (Done(value), rest) ⇒
            val (head, tail) = queue.dequeue
            run(head(value)(rest), tail)
          case (Cont(f), rest) ⇒
            (Cont(Chain(f, queue)), rest)
          case _ ⇒ result
        }
      }
      run(cur(input), queue).asInstanceOf[(Iteratee[A], Input)]
    }
  }

}

object IOManager {
  val global = akka.actor.Actor.actorOf(new IOManager()).start
}

// TODO: Support a pool of workers
class IOManager(bufferSize: Int = 8192) extends Actor {
  import SelectionKey.{ OP_READ, OP_WRITE, OP_ACCEPT, OP_CONNECT }
  import IOWorker._

  var worker: IOWorker = _

  override def preStart {
    worker = new IOWorker(self, bufferSize, self.dispatcher)
  }

  def receive = {
    case IO.Listen(server, address) ⇒
      val channel = ServerSocketChannel open ()
      channel configureBlocking false
      channel.socket bind (address, 1000) // TODO: make backlog configurable
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

  override def postStop {
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

private[akka] class IOWorker(ioManager: ActorRef, val bufferSize: Int, dispatcher: akka.dispatch.MessageDispatcher) {
  import SelectionKey.{ OP_READ, OP_WRITE, OP_ACCEPT, OP_CONNECT }
  import IOWorker._

  type ReadChannel = ReadableByteChannel with SelectableChannel
  type WriteChannel = WritableByteChannel with SelectableChannel

  implicit val optionIOManager: Some[ActorRef] = Some(ioManager)

  def apply(request: Request): Unit =
    addRequest(request)

  // private

  private val selector: Selector = Selector open ()

  private val _requests = new AtomicReference(List.empty[Request])

  private val _running = new AtomicBoolean(false)

  private var accepted = Map.empty[IO.ServerHandle, Queue[SelectableChannel]].withDefaultValue(Queue.empty)

  private var channels = Map.empty[IO.Handle, SelectableChannel]

  private var writes = Map.empty[IO.WriteHandle, Queue[ByteBuffer]].withDefaultValue(Queue.empty)

  private var closing = Set.empty[IO.Handle]

  private val buffer = ByteBuffer.allocate(bufferSize)

  val select = { () ⇒
    if (selector.isOpen) {
      // TODO: Make select behaviour configurable. Blocking 1ms reduces allocations during idle times
      //selector selectNow ()
      selector select 1
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
        case Close(handle: IO.WriteHandle) ⇒
          if (writes contains handle) {
            closing += handle
          } else {
            cleanup(handle, None)
          }
        case Close(handle) ⇒
          cleanup(handle, None)
        case Shutdown ⇒
          channels.keys foreach (handle ⇒ cleanup(handle, None))
          selector.close
      }
      if (channels.isEmpty && (_requests.get eq Nil)) {
        stop()
        if (_requests.get ne Nil) start()
      }
      run()
    } else stop()
  }

  def start() {
    if (!(_running.get || _running.getAndSet(true))) run()
  }

  def stop() {
    _running.set(false)
  }

  def run() { if (_running.get) dispatcher dispatchTask select }

  private def process(key: SelectionKey) {
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

  private def cleanup(handle: IO.Handle, cause: Option[Exception]) {
    closing -= handle
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
        }
      case None ⇒
    }
  }

  private def setOps(handle: IO.Handle, ops: Int): Unit =
    channels(handle) keyFor selector interestOps ops

  private def addOps(handle: IO.Handle, ops: Int) {
    val key = channels(handle) keyFor selector
    val cur = key.interestOps
    key interestOps (cur | ops)
  }

  private def removeOps(handle: IO.Handle, ops: Int) {
    val key = channels(handle) keyFor selector
    val cur = key.interestOps
    key interestOps (cur - (cur & ops))
  }

  private def connect(socket: IO.SocketHandle, channel: SocketChannel) {
    if (channel.finishConnect) {
      removeOps(socket, OP_CONNECT)
      socket.owner ! IO.Connected(socket)
    } else {
      cleanup(socket, None) // TODO: Add a cause
    }
  }

  @tailrec
  private def accept(server: IO.ServerHandle, channel: ServerSocketChannel) {
    val socket = channel.accept
    if (socket ne null) {
      socket configureBlocking false
      accepted += (server -> (accepted(server) enqueue socket))
      server.owner ! IO.NewClient(server)
      accept(server, channel)
    }
  }

  @tailrec
  private def read(handle: IO.ReadHandle, channel: ReadChannel) {
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
  private def write(handle: IO.WriteHandle, channel: WriteChannel) {
    val queue = writes(handle)
    if (queue.nonEmpty) {
      val (buf, bufs) = queue.dequeue
      val writeLen = channel write buf
      if (buf.remaining == 0) {
        if (bufs.isEmpty) {
          if (closing(handle)) {
            cleanup(handle, None)
          } else {
            writes -= handle
            removeOps(handle, OP_WRITE)
          }
        } else {
          writes += (handle -> bufs)
          write(handle, channel)
        }
      }
    }
  }

  @tailrec
  private def addRequest(req: Request) {
    val requests = _requests.get
    if (_requests compareAndSet (requests, req :: requests))
      start()
    else
      addRequest(req)
  }
}
