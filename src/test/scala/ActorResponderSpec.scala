package net.fyrie.redis

import Commands._
import handlers._

import org.specs._
import specification.Context

import akka.actor._
import Actor._
import akka.dispatch._

class ActorResponderSpec extends Specification {
  var r: RedisClient = _

  val empty = new Context {
    before {
      r = new RedisClient
      r send flushdb
    }
    after {
      r send flushdb
      r.disconnect
    }
  }

  "sending requests from an actor" ->- empty should {
    "return simple requests" in {
      val future = new DefaultCompletableFuture[List[Option[Any]]](5000)
      val actor = actorOf(new MatchingActor(r,
                                            future,
                                            List(Result(()),
                                                 Result(Option("testval1"))))).start
      actor ! set("testkey1", "testval1")
      actor ! get("testkey1")
      future.await
      future.result must_== (Some(List.fill(2)(None)))
      actor.stop
    }
    "return multibulk requests" in {
      val future = new DefaultCompletableFuture[List[Option[Any]]](5000)
      val actor = actorOf(new MatchingActor(r,
                                            future,
                                            List(Result(()),
                                                 Result(Option("testval1")),
                                                 Result(1),
                                                 Result(2),
                                                 Result(3),
                                                 Result(4),
                                                 Result(Option("testval2")),
                                                 Result(Option(3)),
                                                 Result(Option("testval5")),
                                                 Result(Option("testval4")),
                                                 Result(Option("testval3")),
                                                 Result(Option("testval3")),
                                                 Result(2)
                                               ))).start
      actor ! set("testkey1", "testval1")
      actor ! get("testkey1")
      actor ! lpush("testkey2", "testval2")
      actor ! lpush("testkey2", "testval3")
      actor ! lpush("testkey2", "testval4")
      actor ! lpush("testkey2", "testval5")
      actor ! rpop("testkey2")
      actor ! lrange("testkey2", 0, -1)
      actor ! rpop("testkey2")
      actor ! llen("testkey2")
      future.await
      future.result must_== (Some(List.fill(13)(None)))
      actor.stop
    }
  }
}

class MatchingActor(r: RedisClient, future: CompletableFuture[List[Option[Any]]], var expectList: List[Response[_]]) extends Actor {

  self.dispatcher = Dispatchers.globalHawtDispatcher

  var results: List[Option[Any]] = Nil

  def expect[A](in: Response[A]): Unit = {
    expectList match {
      case h :: t =>
        expectList = t
        results = (if (in == h && in.manifest == h.manifest) None else Some((in, in.manifest.toString, h.manifest.toString))) :: results
      case Nil =>
        results = Some(in) :: results
    }
    if (expectList == Nil) future.completeWithResult(results)
  }

  def receive = {
    case cmd: Command[_] =>
      r ! cmd
    case Result(MultiParser(length, parser)) =>
      expect(Result(length))
    case LazyResponse(res: Response[_]) =>
      expect(res)
    case res: Response[_] =>
      expect(res)
  }
}
