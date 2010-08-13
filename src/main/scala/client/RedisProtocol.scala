package net.fyrie.redis

import scala.collection.mutable.ArrayBuilder

sealed trait Reply[T] {
  def marker: Char

  def parse(s: String): T

  def parse(r: RedisStreamReader, s: String): T = parse(s)

}

sealed abstract class ReplyBase[T] extends Reply[T]

abstract class ReplyProxy[T, U: ReplyBase] extends Reply[T] {
  val underlying = implicitly[Reply[U]]
  def marker = underlying.marker
  def parse(s: String) = transform(underlying.parse(s))
  override def parse(r: RedisStreamReader, s: String) = transform(underlying.parse(r,s))
  def transform(in: U): T
}

object replies {

  implicit object NoReply extends ReplyBase[Nothing] {
    val marker = ' '

    def parse(s: String): Nothing = error("Can't parse No Reply")
  }

  type Status = String

  implicit object StatusReply extends ReplyBase[Status] {
    val marker = '+'

    def parse(s: String): Status = s
  }

  type OkStatus = Unit

  implicit object OkStatusReply extends ReplyProxy[Unit, Status] {
    def transform(in: Status): Unit = if (in != "OK") throw new RedisProtocolException("Expected 'OK' reply, got: "+in)
  }

  implicit object IntReply extends ReplyBase[Int] {
    val marker = ':'

    def parse(s: String): Int = s.toInt
  }

  type IntAsBoolean = Boolean

  implicit object IntAsBooleanReply extends ReplyProxy[IntAsBoolean, Int] {
    def transform(in: Int): IntAsBoolean = in >= 1
  }

  implicit object LongReply extends ReplyBase[Long] {
    val marker = ':'

    def parse(s: String): Long = s.toLong
  }

  type Bulk = Option[Array[Byte]]

  implicit object BulkReply extends ReplyBase[Bulk] {
    val marker = '$'

    def parse(s: String): Bulk = error("Must use parse(RedisStreamReader, String)")

    override def parse(r: RedisStreamReader, s: String): Bulk =
      s.toInt match {
        case -1 => None
        case l =>
          val bytes = r.readBulk(l)
          Some(bytes)
      }
  }

  type MultiBulk = Option[Seq[Bulk]]

  implicit object MultiBulkReply extends ReplyBase[MultiBulk] {
    val marker = '*'

    def parse(s: String): MultiBulk = error("Must use parse(RedisStreamReader, String)")

    override def parse(r: RedisStreamReader, s: String): MultiBulk =
      s.toInt match {
        case -1 => None
        case n =>  Some(List.fill[Bulk](n)(None).map { i => r.read(BulkReply) })
      }

  }

  type MultiBulkAsSet = Option[Set[Array[Byte]]]

  implicit object MultiBulkAsSetReply extends ReplyProxy[MultiBulkAsSet, MultiBulk] {
    def transform(in: MultiBulk): MultiBulkAsSet = in.map(_.flatten.toSet)
  }

  type MultiBulkAsMap = Option[Map[Array[Byte], Array[Byte]]]

  implicit object MultiBulkAsMapReply extends ReplyProxy[MultiBulkAsMap, MultiBulk] {
    def transform(in: MultiBulk): MultiBulkAsMap = in.map(_.toSeq.grouped(2).collect{
      case Seq(Some(k),Some(v)) => (k,v)
    }.toMap)
  }

  type MultiBulkWithScores = Option[Seq[(Array[Byte], Double)]]

  implicit object MultiBulkWithScoresReply extends ReplyProxy[MultiBulkWithScores, MultiBulk] {
    def transform(in: MultiBulk): MultiBulkWithScores = in.map(_.toSeq.grouped(2).collect{
      case Seq(Some(k),Some(v)) => (k,new String(v).toDouble)
    }.toSeq)
  }

  type MultiBulkAsFlat = Option[Seq[Array[Byte]]]

  implicit object MultiBulkAsFlatReply extends ReplyProxy[MultiBulkAsFlat, MultiBulk] {
    def transform(in: MultiBulk): MultiBulkAsFlat = in.map(_.flatten)
  }

  type MultiExec = Option[Seq[_]]

  final class MultiExecReply(replyList: Seq[Reply[_]]) extends ReplyBase[MultiExec] {
    val marker = '+'

    def parse(s: String): MultiExec = error("Must use parse(RedisStreamReader, String)")

    override def parse(r: RedisStreamReader, s: String): MultiExec = {
      if (s != "OK") throw new Exception("Expected 'OK' reply, got: "+s)
      replyList.foreach(x => r.read(QueuedStatusReply))
      r.read(ExecReply)
    }

    object QueuedStatusReply extends ReplyProxy[Unit, Status] {
      def transform(in: Status): Unit = if (in != "QUEUED") throw new RedisProtocolException("Expected 'QUEUED' reply, got: "+in)
    }

    object ExecReply extends ReplyBase[MultiExec] {
      val marker = '*'

      def parse(s: String): MultiExec = error("Must use parse(RedisStreamReader, String)")

      override def parse(r: RedisStreamReader, s: String): MultiExec = {
        s.toInt match {
          case -1 => None
          case n =>  Some(replyList.map(x => r.read(x)))
        }
      }
    }
  }

}

abstract class Command[T](implicit val replyHandler: Reply[T]) extends Product {
  def name: Array[Byte] = productPrefix.toUpperCase.getBytes
  def args: Seq[Array[Byte]] = productIterator.map{
    case b: Array[Byte] => b
    case s: String => s.getBytes
    case i: Int => Helpers.getBytes(i)
    case l: Long => Helpers.getBytes(l)
    case d: Double => Helpers.getBytes(d)
      case x => error("Unable to serialize: "+x)
  }.toSeq
  def toBytes: Array[Byte] = Helpers.cmd(name +: args)
}

object Helpers {
  def getBytes(i: Int): Array[Byte] = i.toString.getBytes
  def getBytes(l: Long): Array[Byte] = l.toString.getBytes
  def getBytes(d: Double): Array[Byte] = {
    if (d.isInfinity) {
      if (d > 0.0) "+inf" else "-inf"
    } else {
      d.toString
    }
  }.getBytes
  def getBytes(d: Double, inclusive: Boolean): Array[Byte] = {
    (if (inclusive) ("") else ("(")) + {
      if (d.isInfinity) {
        if (d > 0.0) "+inf" else "-inf"
      } else {
        d.toString
      }
    }
  }.getBytes

  object cmd {
    val LS     = "\r\n".getBytes.toList
    def apply(args: Seq[Array[Byte]]): Array[Byte] = {
      val b = new ArrayBuilder.ofByte
      b ++= "*%d".format(args.size).getBytes
      b ++= LS
      args foreach { arg =>
        b ++= "$%d".format(arg.size).getBytes
        b ++= LS
        b ++= arg
        b ++= LS
      }
      b.result
    }
  }

}
