package net.fyrie.redis

import handlers._

import se.scalablesolutions.akka.actor.{ Actor, ActorRef }
import se.scalablesolutions.akka.dispatch._

import org.fusesource.hawtdispatch.ScalaDispatch._

sealed trait Responder {
  val handler: Handler[_]

  def apply(in: RedisType): Seq[Responder]
}

object Responder {
  def apply(handler: Handler[_]): Responder = NoResponder(handler)
  def apply(handler: Handler[_], target: ActorRef): Responder = ActorResponder(handler, target)
  def apply(handler: Handler[_], future: CompletableFuture[Any]): Responder = FutureResponder(handler, future)
  def apply(handler: Handler[_], target: Option[ActorRef], future: Option[CompletableFuture[Any]]): Responder =
    future.map(apply(handler, _)) orElse target.map(apply(handler, _)) getOrElse apply(handler)
}

final case class ActorResponder(handler: Handler[_], target: ActorRef) extends Responder {
  def apply(in: RedisType): Seq[Responder] = error("not implmented")
}

final case class FutureResponder(handler: Handler[_], future: CompletableFuture[Any]) extends Responder {
  def apply(in: RedisType): Seq[Responder] = handler match {
    case bh: Bulk[_] =>
      globalQueue ^ (complete(bh.parse(in)))
      Nil
    case sh: SingleHandler[_] =>
      complete(sh.parse(in))
      Nil
    case MultiExec(handlers) => Nil
      in match {
        case RedisMulti(x) => Nil
          val hfs = handler.handlers.toStream zip Stream.continually(new DefaultCompletableFuture[Any](5000))
          complete(Response(Some(hfs.map(_._2))))
          hfs.map(hf => FutureResponder(hf._1, hf._2))
/*          val futures = handler.handlers.toStream take length map (h => (h, h.mkFuturePair))
          future.completeWithResult(Some(futures.map(_._2._2)))
          futures.map(hf => FutureResponder(hf._1, hf._2._1))*/
        case _ => Stream.continually(NoResponder(QueuedStatus)).take(handlers.length).foldLeft(List[Responder](this)){case (l,q) => q :: l}
      }
    case mh: MultiHandler[_] =>
      in match {
        case RedisMulti(Some(length)) =>
          val hfs = handler.handlers zip Stream.continually(new DefaultCompletableFuture[Any](5000)).take(length)
          complete(Response(Some(hfs.map(_._2))))
          hfs.map(hf => FutureResponder(hf._1, hf._2))
        case RedisMulti(None) =>
          complete(Result(None))
          Nil
        case x => error("ERROR")
      }
  }

  def complete(response: Response[_]): Unit =
    response.fold(future.completeWithResult(_), future.completeWithException(_))
}

final case class NoResponder(handler: Handler[_]) extends Responder {
  def apply(in: RedisType): Seq[Responder] = in match {
    case RedisMulti(Some(length)) =>
      handler.handlers.take(length).map(NoResponder(_))
    case RedisMulti(None) => Nil
    case x =>
      handler match {
        case MultiExec(handlers) =>
          Stream.continually(NoResponder(QueuedStatus)).take(handlers.length).foldLeft(List[Responder](this)){case (l,q) => q :: l}
        case _ => Nil
      }
  }
}
