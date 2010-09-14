package net.fyrie.redis

import scala.collection.mutable.ArrayBuilder

class Reply[T](handler: (RedisStreamReader) => T) {
  def apply(r: RedisStreamReader): T = handler(r)
}

package object replies {
  implicit object NoReply extends Reply[Nothing](r => error("Can't parse No Reply"))

  type Status = String
  implicit object StatusReply extends Reply[Status](status)

  type OkStatus = Unit
  implicit object OkStatusReply extends Reply[Unit](r => verify(status(r), "OK"))

  implicit object IntReply extends Reply[Int](integer(_).toInt)

  type IntAsBoolean = Boolean
  implicit object BooleanReply extends Reply[IntAsBoolean](integer(_) >= 1L)

  implicit object LongReply extends Reply[Long](integer)

  type Bulk = Option[Array[Byte]]
  implicit object BulkReply extends Reply[Bulk](bulk)

  type MultiBulk = Option[Seq[Bulk]]
  implicit object MultiBulkReply extends Reply[MultiBulk](multibulk(_).map(_.toList))

  type MultiBulkAsSet = Option[Set[Array[Byte]]]
  implicit object MultiBulkAsSetReply extends Reply[MultiBulkAsSet](multibulk(_).map(_.flatten.toSet))

  type MultiBulkAsMap = Option[Map[Array[Byte], Array[Byte]]]
  implicit object MultiBulkAsMapReply extends Reply[MultiBulkAsMap](
    multibulk(_).map(_.grouped(2).collect{case Seq(Some(k),Some(v)) => (k,v)}.toMap))

  type MultiBulkWithScores = Option[Seq[(Array[Byte], Double)]]
  implicit object MultiBulkWithScoresReply extends Reply[MultiBulkWithScores](
    multibulk(_).map(_.grouped(2).collect{case Seq(Some(k),Some(v)) => (k,new String(v).toDouble)}.toSeq))

  type MultiBulkAsFlat = Option[Seq[Array[Byte]]]
  implicit object MultiBulkAsFlatReply extends Reply[MultiBulkAsFlat](multibulk(_).map(_.flatten.toList))

  type MultiExec = Option[Seq[_]]
  final class MultiExecReply(replyList: Seq[Reply[_]]) extends Reply[MultiExec]({ r =>
    OkStatusReply(r)
    replyList.foreach(x => QueuedStatusReply(r))
    multiexec(r, replyList)
  })

  object QueuedStatusReply extends Reply[Unit](r => verify(status(r), "QUEUED"))

  private def verify(in: String, expect: String): Unit =
    if (in != expect) throw new RedisProtocolException("Expected '"+expect+"' reply, got: "+in)

  private def parse(marker: Char, r: RedisStreamReader): String = {
    val reply = r.readReply
    val m = reply(0).toChar
    val result = new String(reply, 1, reply.size - 1)
    if (m == marker) {
      result
    } else {
      if (m == '-') (throw new RedisErrorException(result))
      throw new RedisProtocolException("Got '" + m + result + "' as reply")
    }
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
  def args: Iterator[Any] = productIterator
  def toBytes: Array[Byte] = Command.create((Iterator.single(name.getBytes) ++ args.map(serializers)).toSeq)
  def serializers: PartialFunction[Any, Array[Byte]] = {
    case b: Array[Byte] => b
    case d: Double => Command.serializeDouble(d)
    case x => x.toString.getBytes("UTF-8")
  }
  def withSerializers(in: PartialFunction[Any, Array[Byte]]): Command[T] =
    new CommandWrapper(this) {
      override def serializers = in orElse underlying.serializers
    }
}

class CommandWrapper[T](val underlying: Command[T]) extends Command[T]()(underlying.replyHandler) {
  override def name = underlying.name
  override def args = underlying.args
  override def productPrefix = underlying.productPrefix
  def productArity: Int = underlying.productArity
  def productElement(n: Int): Any = underlying.productElement(n)
  def canEqual(that: Any): Boolean = underlying.canEqual(that)
}

object Command {
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

package commands {
  package object helpers {
    def arg1(value: Any): Iterator[Any] = Iterator.single(value)
    def argN1(value: Iterable[Any]): Iterator[Any] = value.iterator
    def argN1(name: Any, value: Iterable[Any]): Iterator[Any] = value.iterator.flatMap(Iterator(name, _))
    def argN2(value: Iterable[Product2[Any,Any]]): Iterator[Any] = value.iterator.flatMap(_.productIterator)
    def argN2(name: Any, value: Iterable[Product2[Any,Any]]): Iterator[Any] = value.iterator.flatMap(Iterator.single(name) ++ _.productIterator)
  }
}
