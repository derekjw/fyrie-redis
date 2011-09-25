package akka.util.iteratee

import akka.util.ByteString

import scala.collection.immutable.Queue

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
  private[iteratee] object Chain {
    def apply[A](f: ByteString ⇒ Iteratee[A]) = new Chain[A](f, Queue.empty)
    def apply[A, B](f: ByteString ⇒ Iteratee[A], k: A ⇒ Iteratee[B]) = new Chain[B](f, Queue(k.asInstanceOf[Any ⇒ Iteratee[Any]]))
  }

  private[iteratee] final class Chain[A] private (cur: ByteString ⇒ Iteratee[Any], queue: Queue[Any ⇒ Iteratee[Any]]) extends (ByteString ⇒ Iteratee[A]) {

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
