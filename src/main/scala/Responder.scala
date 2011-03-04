package net.fyrie.redis

import handlers._
import utils._
import RedisType._
import akka.actor.{ Actor, ActorRef }
import akka.dispatch._

import org.fusesource.hawtdispatch.ScalaDispatch._

sealed trait Responder {
  def handler: Handler[_]

  def apply[T: RedisType: Manifest](in: T): Seq[Responder]
}

object Responder {
  def apply(handler: Handler[_]): Responder = NoResponder(handler)
  def apply(handler: Handler[_], target: ActorRef): Responder = ActorResponder(handler, target)
  def apply(handler: Handler[_], future: CompletableFuture[Any]): Responder = FutureResponder(handler, future)
  def apply(handler: Handler[_], target: Option[ActorRef], future: Option[CompletableFuture[Any]]): Responder =
    future.map(apply(handler, _)) orElse target.map(apply(handler, _)) getOrElse apply(handler)
}

final case class ActorResponder(handler: Handler[_], target: ActorRef) extends Responder {
  def apply[T: RedisType: Manifest](in: T): Seq[Responder] = (implicitly[RedisType[T]], handler) match {
    case (RedisError,_) =>
      target ! Error(in)
      Nil
    case (RedisBulk, bh: Bulk[_]) =>
      target ! bh.lazyParse(in)
      Nil
    case (_, sh: SingleHandler[_,_]) =>
      ifType(in, sh.inputManifest) { x =>
        target ! sh.parse(x)
      } { x =>
        target ! Error(new RedisProtocolException("Incorrect Type: "+x))
      }
      Nil
    case (RedisString, MultiExec(handlers)) if in == "OK" =>
      Stream.continually(NoResponder(QueuedStatus)).take(handlers.length).foldLeft(List[Responder](this)){case (l,q) => q :: l}
    case (RedisMulti, mh: MultiHandler[_]) =>
      target ! mh.parser(in)
      in map (mh.handlers.take(_).map(h => ActorResponder(h, target))) getOrElse Nil
    case (_, _) =>
      target ! Error(new RedisProtocolException("Incorrect value: "+in+" for handler: "+handler))
      Nil
  }
}
object FutureResponder {
  def mkFutures[A: Manifest](in: Handler[A]): (CompletableFuture[Any], Future[A]) = {
    in match {
      case sh: SingleHandler[_,_] =>
        val future = new ResponseFuture[A](5000)(in.manifest)
        (future.asInstanceOf[CompletableFuture[Any]], future)
      case mh: MultiHandler[_] =>
        val future = new ResponseFuture[Option[Stream[Future[Any]]]](5000)
        (future.asInstanceOf[CompletableFuture[Any]], future.map(x => mh.parse(x.map(_.map(f => futureToResponse(f))))))
    }
  }

  def futureToResponse[A: Manifest](in: Future[A]): Response[A] = in match {
    case r: ResponseFuture[_] => r.toResponse
    case _ => in.await.result.map(Result(_)).orElse(in.exception.map(Error(_))).getOrElse(Error(error("ERROR")))
  }
}

final case class FutureResponder(handler: Handler[_], future: CompletableFuture[Any]) extends Responder {
  import FutureResponder.mkFutures

  def apply[T: RedisType: Manifest](in: T): Seq[Responder] = (implicitly[RedisType[T]], handler) match {
    case (RedisError,_) =>
      complete(Error(in))
    case (RedisBulk, bh: Bulk[_]) =>
      globalQueue ^ (complete(bh.parse(in)))
      Nil
    case (_, sh: SingleHandler[_,_]) =>
      ifType(in, sh.inputManifest) { x =>
        complete(sh.parse(x))
      } { x =>
        complete(Error(new RedisProtocolException("Incorrect Type: "+x)))
      }
    case (RedisString, MultiExec(handlers)) if in == "OK" =>
      Stream.continually(NoResponder(QueuedStatus)).take(handlers.length).foldLeft(List[Responder](this)){case (l,q) => q :: l}
    case (RedisMulti, mh: MultiHandler[_]) if in.isDefined =>
      val hfs = mh.handlers.take(in.get).toStream map (h => (h, mkFutures(h)))
      complete(Response(Some(hfs.map(_._2._2))))
      hfs.map(hf => FutureResponder(hf._1, hf._2._1))
    case (RedisMulti, mh: MultiHandler[_]) =>
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
  def apply[T: RedisType: Manifest](in: T): Seq[Responder] = implicitly[RedisType[T]] match {
    case RedisMulti if in.isDefined =>
      handler.handlers.take(in.get).map(NoResponder(_))
    case RedisMulti => Nil
    case x =>
      handler match {
        case MultiExec(handlers) =>
          Stream.continually(NoResponder(QueuedStatus)).take(handlers.length).foldLeft(List[Responder](this)){case (l,q) => q :: l}
        case _ => Nil
      }
  }
}
