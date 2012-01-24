package net.fyrie.redis

import org.specs2._
import org.specs2.specification._
import org.specs2.execute._
import akka.actor.ActorSystem
import akka.dispatch.{ Future, Await }
import akka.util.Duration

object TestSystem {
  val config = com.typesafe.config.ConfigFactory.parseString("""
      akka {
        event-handlers = ["akka.testkit.TestEventListener"]
        loglevel = "WARNING"
        actor {
          default-dispatcher {
            core-pool-size-min = 4
            core-pool-size-factor = 2.0
            throughput = 10
          }
        }
      }
      """)

  val system = ActorSystem("TestSystem", config)
}

trait TestClient { self: mutable.Specification ⇒

  implicit def futureResult[T](future: Future[T])(implicit toResult: T ⇒ Result): Result =
    Await.result(future map toResult, akka.util.Timeout(1000).duration) //Duration.Inf)

  implicit val system = TestSystem.system

  implicit val arguments = args(sequential = true)

  val config = RedisClientConfig(connections = 1)

  def client = new AroundOutside[RedisClient] {

    val r = RedisClient(config = config)
    r.sync.flushall

    def around[T <% Result](t: ⇒ T) = {
      val result = t
      r.sync.flushall
      r.disconnect
      result
    }

    def outside: RedisClient = r

  }

}

trait UnstableClient { self: mutable.Specification ⇒

  implicit val system = TestSystem.system

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
