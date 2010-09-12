package net.fyrie.redis

import scala.collection.mutable.ArrayBuilder

trait Reply[T] {
  def apply(r: RedisStreamReader): T
}

package object replies {

  implicit object NoReply extends Reply[Nothing] {
    def apply(r: RedisStreamReader): Nothing = error("Can't parse No Reply")
  }

  type Status = String

  implicit object StatusReply extends Reply[Status] {
    def apply(r: RedisStreamReader): Status = status(r)
  }

  type OkStatus = Unit

  implicit object OkStatusReply extends Reply[Unit] {
    def apply(r: RedisStreamReader): Unit = {
      val result = status(r)
      if (result != "OK") throw new RedisProtocolException("Expected 'OK' reply, got: "+result)
    }
  }

  implicit object IntReply extends Reply[Int] {
    def apply(r: RedisStreamReader): Int = integer(r).toInt
  }

  type IntAsBoolean = Boolean

  implicit object BooleanReply extends Reply[Boolean] {
    def apply(r: RedisStreamReader): Boolean = integer(r) >= 1L
  }

  implicit object LongReply extends Reply[Long] {
    def apply(r: RedisStreamReader): Long = integer(r)
  }

  type Bulk = Option[Array[Byte]]

  implicit object BulkReply extends Reply[Bulk] {
    def apply(r: RedisStreamReader): Bulk = bulk(r)
  }

  type MultiBulk = Option[Seq[Bulk]]

  implicit object MultiBulkReply extends Reply[MultiBulk] {
    def apply(r: RedisStreamReader): MultiBulk = multibulk(r).map(_.toList)
  }

  type MultiBulkAsSet = Option[Set[Array[Byte]]]

  implicit object MultiBulkAsSetReply extends Reply[MultiBulkAsSet] {
    def apply(r: RedisStreamReader): MultiBulkAsSet = multibulk(r).map(_.flatten.toSet)
  }

  type MultiBulkAsMap = Option[Map[Array[Byte], Array[Byte]]]

  implicit object MultiBulkAsMapReply extends Reply[MultiBulkAsMap] {
    def apply(r: RedisStreamReader): MultiBulkAsMap = multibulk(r).map(_.grouped(2).collect{
      case Seq(Some(k),Some(v)) => (k,v)
    }.toMap)
  }

  type MultiBulkWithScores = Option[Seq[(Array[Byte], Double)]]

  implicit object MultiBulkWithScoresReply extends Reply[MultiBulkWithScores] {
    def apply(r: RedisStreamReader): MultiBulkWithScores = multibulk(r).map(_.grouped(2).collect{
      case Seq(Some(k),Some(v)) => (k,new String(v).toDouble)
    }.toSeq)
  }

  type MultiBulkAsFlat = Option[Seq[Array[Byte]]]

  implicit object MultiBulkAsFlatReply extends Reply[MultiBulkAsFlat] {
    def apply(r: RedisStreamReader): MultiBulkAsFlat = multibulk(r).map(_.flatten.toList)
  }

  type MultiExec = Option[Seq[_]]

  final class MultiExecReply(replyList: Seq[Reply[_]]) extends Reply[MultiExec] {
    def apply(r: RedisStreamReader): MultiExec = {
      OkStatusReply(r)
      replyList.foreach(x => QueuedStatusReply(r))
      multiexec(r, replyList)
    }
  }

  object QueuedStatusReply extends Reply[Unit] {
    def apply(r: RedisStreamReader): Unit = {
      val result = status(r)
      if (result != "QUEUED") throw new RedisProtocolException("Expected 'QUEUED' reply, got: "+result)
    }
  }

  private def parse(marker: Char, r: RedisStreamReader): String = {
    val reply = r.readReply
    val m = reply(0).toChar
    val s = new String(reply, 1, reply.size - 1)
    if (m != marker) {
      if (m == '-') (throw new RedisErrorException(s))
      //reconnect
      throw new RedisProtocolException("Got '" + m + s + "' as reply")
    }
    s
  }

  private def status(r: RedisStreamReader): String = parse('+', r)

  private def integer(r: RedisStreamReader): Long = parse(':', r).toLong

  private def bulk(r: RedisStreamReader): Option[Array[Byte]] =
    parse('$', r).toInt match {
      case -1 => None
      case l =>
        val bytes = r.readBulk(l)
        Some(bytes)
    }

  private def multibulk(r: RedisStreamReader): Option[Seq[Option[Array[Byte]]]] =
    parse('*', r).toInt match {
      case -1 => None
      case n =>  Some(List.fill[Option[Array[Byte]]](n)(None).toStream.map { i => bulk(r) })
    }

  private def multiexec(r: RedisStreamReader, replyList: Seq[Reply[_]]): Option[Seq[_]] =
    parse('*', r).toInt match {
      case -1 => None
      case n =>  Some(replyList.map(_(r)))
    }
}

abstract class Command[T](implicit val replyHandler: Reply[T]) extends Product {
  def name: String = productPrefix.toUpperCase
  def args: Seq[Any] = productIterator.toSeq
  def toBytes: Array[Byte] = Command.create(Command.serialize(name +: args))
}

object Command {
  def serialize(seq: Seq[Any]): Seq[Array[Byte]] = seq.flatMap{
    case b: Array[Byte] => Seq(b)
    case d: Double => Seq(serializeDouble(d))
    case s: TraversableOnce[_] => serialize(s.toSeq)
    case p: Product => serialize(p.productIterator.toSeq)
    case x => Seq(x.toString.getBytes("UTF-8"))
  }

  def serializeDouble(d: Double, inclusive: Boolean = true): Array[Byte] = {
    (if (inclusive) ("") else ("(")) + {
      if (d.isInfinity) {
        if (d > 0.0) "+inf" else "-inf"
      } else {
        d.toString
      }
    }
  }.getBytes

  val EOL = "\r\n".getBytes.toSeq

  def create(args: Seq[Array[Byte]]): Array[Byte] = {
    val b = new ArrayBuilder.ofByte
    b ++= "*%d".format(args.size).getBytes
    b ++= EOL
    args foreach { arg =>
      b ++= "$%d".format(arg.size).getBytes
      b ++= EOL
      b ++= arg
      b ++= EOL
    }
    b.result
  }
}
