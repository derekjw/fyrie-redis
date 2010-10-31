package net.fyrie.redis

import handlers._
import utils._

import se.scalablesolutions.akka.actor.{ Actor, ActorRef }
import se.scalablesolutions.akka.dispatch._

import org.fusesource.hawtdispatch.ScalaDispatch._

sealed trait Responder {
  def handler: Handler[_]

  def apply(in: RedisType[_]): Seq[Responder]
}

object Responder {
  def apply(handler: Handler[_]): Responder = NoResponder(handler)
  def apply(handler: Handler[_], target: ActorRef): Responder = ActorResponder(handler, target)
  def apply(handler: Handler[_], future: CompletableFuture[Any]): Responder = FutureResponder(handler, future)
  def apply(handler: Handler[_], target: Option[ActorRef], future: Option[CompletableFuture[Any]]): Responder =
    future.map(apply(handler, _)) orElse target.map(apply(handler, _)) getOrElse apply(handler)
}

final case class ActorResponder(handler: Handler[_], target: ActorRef) extends Responder {
  def apply(in: RedisType[_]): Seq[Responder] = (in, handler) match {
    case (RedisError(err),_) =>
      target ! Error(err)
      Nil
    case (b: RedisBulk, bh: Bulk[_]) =>
      target ! (b, bh)
      Nil
    case (_, sh: SingleHandler[_,_]) =>
      checkType(in)(sh.inputManifest) match {
        case Some(value) => target ! sh.parse(value)
        case None => target ! Error(new RedisProtocolException("Incorrect Type: "+in))
      }
      Nil
    case (RedisString("OK"), MultiExec(handlers)) =>
      Stream.continually(NoResponder(QueuedStatus)).take(handlers.length).foldLeft(List[Responder](this)){case (l,q) => q :: l}
    case (rm @ RedisMulti(length), mh: MultiHandler[_]) =>
      target ! (rm, mh)
      length map (mh.handlers.take(_).map(h => ActorResponder(h, target))) getOrElse Nil
    case (_, _) =>
      target ! Error(new RedisProtocolException("Incorrect value: "+in+" for handler: "+handler))
      Nil
  }
}
object FutureResponder {
  def mkFutures[A](in: Handler[A]): (CompletableFuture[Any], Future[A]) = {
    in match {
      case sh: SingleHandler[_,_] =>
        val future = new DefaultCompletableFuture[A](5000)
        (future.asInstanceOf[CompletableFuture[Any]], future)
      case mh: MultiHandler[_] =>
        val future = new DefaultCompletableFuture[Option[Stream[Future[Any]]]](5000)
        (future.asInstanceOf[CompletableFuture[Any]], future.map(x => mh.parse(x.map(_.map(f => futureToResponse(f))))))
    }
  }

  def futureToResponse[A](in: Future[A]): Response[A] =
    in.await.result.map(Result(_)).orElse(in.exception.map(Error(_))).getOrElse(Error(error("ERROR")))
}
final case class FutureResponder(handler: Handler[_], future: CompletableFuture[Any]) extends Responder {
  import FutureResponder.mkFutures

  def apply(in: RedisType[_]): Seq[Responder] = (in, handler) match {
    case (RedisError(err),_) =>
      complete(Error(err))
    case (b: RedisBulk, bh: Bulk[_]) =>
      globalQueue ^ (complete(bh.parse(b)))
      Nil
    case (_, sh: SingleHandler[_,_]) =>
      checkType(in)(sh.inputManifest) match {
        case Some(value) => complete(sh.parse(value))
        case None => complete(Error(new RedisProtocolException("Incorrect Type: "+in)))
      }
    case (RedisString("OK"), MultiExec(handlers)) =>
      Stream.continually(NoResponder(QueuedStatus)).take(handlers.length).foldLeft(List[Responder](this)){case (l,q) => q :: l}
    case (RedisMulti(Some(length)), mh: MultiHandler[_]) =>
      val hfs = mh.handlers.take(length).toStream map (h => (h, mkFutures(h)))
      complete(Response(Some(hfs.map(_._2._2))))
      hfs.map(hf => FutureResponder(hf._1, hf._2._1))
    case (RedisMulti(None), mh: MultiHandler[_]) =>
      complete(Result(None))
    case (_, _) =>
      complete(Error(new RedisProtocolException("Incorrect value: "+in+" for handler: "+handler)))
  }

  def complete(response: Response[_]): List[Nothing] = {
    response.fold(future.completeWithResult(_), future.completeWithException(_))
    Nil
  }
}

final case class NoResponder(handler: Handler[_]) extends Responder {
  def apply(in: RedisType[_]): Seq[Responder] = in match {
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
