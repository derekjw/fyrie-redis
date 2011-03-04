package net.fyrie.redis

import Commands._

import org.specs._
import specification.Context

class ExceptionHandlingSpec extends Specification {
  var r: RedisClient = _

  val emptyDb = new Context {
    before {
      r = new RedisClient
      r send flushdb
    }
    after {
      r send flushdb
      r.disconnect
    }
  }

  "redis exceptions" ->- emptyDb should {
    "recover" in {
      "single commands" in {
        r send set("hello", "world")
        (r send get("hello")) must_== Some("world")
        r send rename("hello", "world")
        (r send rename("hello", "world")) must throwA[RedisErrorException]
        (r send get("world")) must_== Some("world")
      }
      "repeated commands" in {
        r send set("hello", "world")
        (r send get("hello")) must_== Some("world")
        r send rename("hello", "world")
        (1 to 10) foreach {i =>
          (r send rename("hello", "world")) must throwA[RedisErrorException]
        }
        (r send get("world")) must_== Some("world")
      }
      "Without iterupting other commands" in {
        "two way" in {
          r send set("testint", "0")
          (1 to 10000) foreach {i => r ! incr("testint")}
          (r send rename("hello", "world")) must throwA[RedisErrorException]
          (1 to 10000) foreach {i => r ! incr("testint")}
          (r send get("testint")) must_== Some("20000")
        }
        "one way" in {
          r send set("testint", "0")
          (1 to 10000) foreach {i => r ! incr("testint")}
          r ! rename("hello", "world")
          (1 to 10000) foreach {i => r ! incr("testint")}
          (r send get("testint")) must_== Some("20000")
        }
      }
      "One way commands" in {
        r send set("testint", "invalid")
        (1 to 10) foreach { i => r ! incr("testint") }
        (r send get("testint")) must_== Some("invalid")
      }
    }
  }
}
