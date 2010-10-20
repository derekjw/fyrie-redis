package net.fyrie.redis

import serialization._
import handlers.{ Handler }
import commands._

import scala.collection.mutable.ArrayBuilder

/**
 * Creates commands to send to a Redis server, performing any needed
 * serialization with the given `Format` and provides a Handler for
 * the server's response. Implementing classes should either be case
 * classes or case objects to allow simple formatting of parameters with
 * very little extra code.
 *
 * @param handler Handler for the Redis server's response to this command
 * @param format  Each command arg gets passed to this formatter to apply
 *                any needed modifications before being sent. A default
 *                Format is supplied implicitly.
 *
 * @tparam A the raw return type of the Redis command
 * @tparam B the parsed return type of the Redis command
 *
 */
abstract class Command[A,B](val handler: Handler[A,B])(implicit format: Format) extends Product {

  /**
   * Provides the name of the Redis command, formatted as upper case.
   * Uses the class name of the implementing class if it is a case
   * class or case object. Override if the class name does not match
   * the Redis command name.
   */
  def name: String = productPrefix.toUpperCase

  /**
   * Takes the unformatted parameters of the implementing case class
   * and returns an Iterator over them. Can handle simple cases, but
   * will need to be overridden for others.
   */
  def args: Iterator[Any] = productIterator

  /**
   * Applies the Format to each arg, prepends the name of the command
   * and returns the command as an `Array[Byte]` to be sent to the
   * Redis server.
   */
  def toBytes: Array[Byte] = Command.create((Iterator.single(name.getBytes) ++ args.map(x => format(x))).toSeq)
}

object Command {

  /**
   * Doubles need special attention when serializing, especially when used
   * with ranges.
   */
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

  /**
   * Takes a Seq (usually a Stream) of individual components of a proper
   * Redis MultiBulk command and concatenates them to conform to Redis'
   * protocol.
   */
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

  /**
   * Helper method for single args.
   */
  def arg1(value: Any): Iterator[Any] = Iterator.single(value)

  /**
   * Helper method for a collection of single args.
   */
  def argN1(value: Iterable[Any]): Iterator[Any] = value.iterator

  /**
   * Helper method for a collection of single args. Places the provided name
   * before each arg.
   */
  def argN1(name: Any, value: Iterable[Any]): Iterator[Any] = value.iterator.flatMap(Iterator(name, _))

  /**
   * Helper method for a collection of paired args.
   */
  def argN2(value: Iterable[Product2[Any, Any]]): Iterator[Any] = value.iterator.flatMap(_.productIterator)

  /**
   * Helper method for a collection of paired args. Places the provided name
   * before each pair of args.
   */
  def argN2(name: Any, value: Iterable[Product2[Any, Any]]): Iterator[Any] = value.iterator.flatMap(Iterator.single(name) ++ _.productIterator)
}

object Commands
  extends StringCommands
  with ListCommands
  with SetCommands
  with SortedSetCommands
  with HashCommands
  with GenericCommands
  with NodeCommands
  with SortTupleCommands
