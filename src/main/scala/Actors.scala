package net.fyrie
package redis
package actors

import messages._
import types._

import akka.actor.{ Actor, ActorRef, IO, IOManager }
import Actor.{ actorOf }
import akka.util.ByteString
import akka.util.cps._

import scala.util.continuations._

final class RedisClientSession(ioManager: ActorRef, host: String, port: Int) extends Actor {

  var socket: IO.SocketHandle = _
  var worker: ActorRef = _

  override def preStart = {
    worker = actorOf(new RedisClientWorker)
    self startLink worker
    socket = IO.connect(ioManager, host, port, worker)
    worker ! Socket(socket)
  }

  def receive = {
    case Request(bytes) =>
      socket write bytes
      worker forward Run
    case MultiRequest(multi, cmds, exec) =>
      socket write multi
      cmds foreach {cmd =>
        socket write cmd._1
      }
      socket write exec
      worker forward MultiRun(cmds.map(_._2))
    case Disconnect =>
      self.stop()
  }

}

final class RedisClientWorker extends Actor with IO {
  import Protocol._

  var socket: IO.SocketHandle = _

  def receiveIO: ReceiveIO = {
    case Socket(handle) =>
      socket = handle
    case Run =>
      val result = readResult
      self reply_? result
    case msg: MultiRun =>
      val multi = readResult
      var promises = msg.promises
      whileC(promises.nonEmpty) {
        val promise = promises.head
        promises = promises.tail
        promise completeWithResult readResult
        () // TODO: fix this in akka.util.cps.whileC
      }
      val exec = readResult
      self reply_? exec
    case Disconnect =>
      socket.close
      self.stop()
  }

  def readResult: RedisType @cps[IO.IOSuspendable[Any]] = {
    val resultType = socket read 1
    resultType.head.toChar match {
      case '+' => readString
      case '-' => readError
      case ':' => readInteger
      case '$' => readBulk
      case '*' => readMulti
      case x => sys.error("Invalid result type: " + x.toByte)
    }
  }

  def readString = {
    val bytes = socket read EOL
    RedisString(bytes.utf8String)
  }

  def readError = {
    val bytes = socket read EOL
    RedisError(bytes.utf8String)
  }

  def readInteger = {
    val bytes = socket read EOL
    RedisInteger(bytes.utf8String.toLong)
  }

  def readBulk = {
    val length = socket read EOL
    matchC(length.utf8String.toInt) {
      case -1 => RedisBulk.notfound
      case 0 => RedisBulk.empty
      case n =>
        val bytes = socket read n
        socket read EOL
        RedisBulk(Some(bytes))
    }
  }

  def readMulti = {
    val count = socket read EOL
    matchC(count.utf8String.toInt) {
      case -1 => RedisMulti.notfound
      case 0 => RedisMulti.empty
      case n =>
        var result = new Array[RedisType](n)
        var i = 0
        repeatC(n) {
          result(i) = readResult
          i += 1
        }
        RedisMulti(Some(result.toList))
    }
  }
}
