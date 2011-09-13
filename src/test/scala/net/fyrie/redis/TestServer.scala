package net.fyrie.redis

import org.specs2._
import specification._
import execute._

trait TestClient { self: mutable.Specification ⇒

  implicit val arguments = args(sequential = true)

  def client = new AroundOutside[RedisClient] {

    val r = RedisClient()
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
