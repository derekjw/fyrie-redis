package net.fyrie.redis
package akka

import ByteStringHelpers._

import commands._

import org.specs._
import specification.Context

class ExceptionHandlingSpec extends Specification {
  var r: AkkaRedisClient = _

  val empty = new Context {
    before {
      r = new AkkaRedisClient("localhost", 16379)
      r send flushdb
    }
    after {
      r send flushdb
      r.disconnect
    }
  }

  "redis exceptions" ->- empty should {
    "recover" in {
      "single commands" in {
        r send set("hello", "world")
        (r send get("hello")).map(fromBytes) must_== Some("world")
        r send rename("hello", "world")
        (r send rename("hello", "world")) must throwA[RedisErrorException]
        (r send get("world")).map(fromBytes) must_== Some("world")
      }
      "repeated commands" in {
        r send set("hello", "world")
        (r send get("hello")).map(fromBytes) must_== Some("world")
        r send rename("hello", "world")
        (1 to 10) foreach {i =>
          (r send rename("hello", "world")) must throwA[RedisErrorException]
        }
        (r send get("world")).map(fromBytes) must_== Some("world")
      }
      "Without iterupting other commands" in {
        "two way" in {
          r send set("testint", "0")
          (1 to 10000) foreach {i => r ! incr("testint")}
          (r send rename("hello", "world")) must throwA[RedisErrorException]
          (1 to 10000) foreach {i => r ! incr("testint")}
          (r send get("testint")).map(fromBytes) must_== Some("20000")
        }
        "one way" in {
          r send set("testint", "0")
          (1 to 10000) foreach {i => r ! incr("testint")}
          r ! rename("hello", "world")
          (1 to 10000) foreach {i => r ! incr("testint")}
          (r send get("testint")).map(fromBytes) must_== Some("20000")
        }
      }
      "One way commands" in {
        r send set("testint", "invalid")
        (1 to 10) foreach { i => r ! incr("testint") }
        (r send get("testint")).map(fromBytes) must_== Some("invalid")
      }
    }
  }
}
