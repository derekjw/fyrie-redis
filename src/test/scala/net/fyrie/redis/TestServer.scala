package net.fyrie.redis

import org.specs2._
import specification._
import execute._

trait TestClient { self: mutable.Specification ⇒

  implicit val arguments = args(sequential = true)

  def client = new AroundOutside[RedisClient] {

    val ioManager = akka.actor.Actor.actorOf(new akka.actor.IOManager()).start
    val r = RedisClient(ioManager = ioManager)
    r.sync.flushall

    def around[T <% Result](t: ⇒ T) = {
      val result = t
      r.sync.flushall
      r.disconnect
      ioManager.stop
      result
    }

    def outside: RedisClient = r

  }

}

trait UnstableClient { self: mutable.Specification ⇒

  implicit val arguments = args(sequential = true)

  def client = new AroundOutside[RedisClient] {

    val r = RedisClient()
    r.sync.flushall

    def around[T <% Result](t: ⇒ T) = {
      val result: Result = r.sync.info.lines.find(_ startsWith "redis_version").map(_.split(Array(':', '.')).tail.map(_.toInt).toList) match {
        case Some(a :: b :: _) if (a > 2) || ((a == 2) && (b > 4)) ⇒ t
        case _ ⇒ skipped
      }
      r.sync.flushall
      r.disconnect
      result
    }

    def outside: RedisClient = r

  }

}
