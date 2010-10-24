package net.fyrie.redis

import handlers._

import se.scalablesolutions.akka.actor.{ Actor, ActorRef }
import se.scalablesolutions.akka.dispatch._

sealed trait Responder {
  val handler: Handler[_, _]

  def apply[T](in: RedisType[T]): Seq[Responder]
}

object Responder {
  def apply(handler: Handler[_, _]): Responder = NoResponder(handler)
  def apply(handler: Handler[_, _], target: ActorRef): Responder = ActorResponder(handler, target)
  def apply(handler: Handler[_, _], future: CompletableFuture[Any]): Responder = FutureResponder(handler, future)
  def apply(handler: Handler[_, _], target: Option[ActorRef], future: Option[CompletableFuture[Any]]): Responder =
    future.map(apply(handler, _)) orElse target.map(apply(handler, _)) getOrElse apply(handler)
}

final case class ActorResponder(handler: Handler[_, _], target: ActorRef) extends Responder {
  def apply[T](in: RedisType[T]): Seq[Responder] = error("not implmented")
}

final case class FutureResponder(handler: Handler[_, _], future: CompletableFuture[Any]) extends Responder {
  def apply[T](in: RedisType[T]): Seq[Responder] = in match {
    case RedisError(err) =>
      future.completeWithException(err)
      Nil
    case RedisMulti(Some(length)) =>
      handler match {
        case MultiExec(handlers) =>
          val futures = handler.childHandlers.toStream take length map (h => (h, h.mkFuturePair))
          future.completeWithResult(Some(futures.map(_._2._2)))
          futures.map(hf => FutureResponder(hf._1, hf._2._1))
        case _ => 
          val futures = Stream.continually(new DefaultCompletableFuture[Any](5000)).take(length)
          future.completeWithResult(Some(futures))
          handler.childHandlers zip futures map (hf => FutureResponder(hf._1, hf._2))
      }
    case RedisMulti(None) =>
      future.completeWithResult(None)
      Nil
    case RedisType(value) =>
      handler match {
        case MultiExec(handlers) =>
          Stream.continually(NoResponder(QueuedStatus)).take(handlers.length).foldLeft(List[Responder](this)){case (l,q) => q :: l}
        case _ => 
          future.completeWithResult(value)
          Nil
      }
  }
}

final case class NoResponder(handler: Handler[_, _]) extends Responder {
  def apply[T](in: RedisType[T]): Seq[Responder] = in match {
    case RedisMulti(Some(length)) =>
      handler.childHandlers.take(length).map(NoResponder(_))
    case RedisMulti(None) => Nil
    case RedisType(_) =>
      handler match {
        case MultiExec(handlers) =>
          Stream.continually(NoResponder(QueuedStatus)).take(handlers.length).foldLeft(List[Responder](this)){case (l,q) => q :: l}
        case _ => Nil
      }
  }
}
