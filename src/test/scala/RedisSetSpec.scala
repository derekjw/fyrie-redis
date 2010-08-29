package net.fyrie.redis
package akka
package collection

import commands._

import org.specs._
import specification.Context

class RedisSetSpec extends Specification {
  implicit val r = new AkkaRedisClient("localhost", 16379)

  implicit def strToBytes(in: String): Array[Byte] = in.getBytes
  implicit def bytesToStr(in: Array[Byte]): String = new String(in)

  val empty = new Context {
    before {
      r !! flushdb
    }
    after {
      r !! flushdb
    }
  }

  "size" ->- empty should {
    "return 0 for empty set" in {
      val set = RedisSet[String]("test set 1")
      set must haveSize(0)
    }
    "return proper values for filled sets" in {
      val set = RedisSet[String]("test set 1")
      set += "Hello" += "World"
      set must haveSize(2)
      set += "Hello"
      set must haveSize(2)
      set += "Hello again"
      set must haveSize(3)
    }
  }
  "spop" ->- empty should {
    "pop from the set" in {
      val set = RedisSet[String]("test set 1")
      val vs = Set("Hello", "World", "test1", "test2", "I'm last")
      set ++= vs
      set must haveSize(5)
      set.spop must beSome.which(_ must beIn(vs))
      set must haveSize(4)
      set.spop must beSome.which(_ must beIn(vs))
      set must haveSize(3)
      set.spop must beSome.which(_ must beIn(vs))
      set must haveSize(2)
      set.spop must beSome.which(_ must beIn(vs))
      set must haveSize(1)
      set.spop must beSome.which(_ must beIn(vs))
      set must haveSize(0)
      set.spop must beNone
      set += "test after empty"
      set.spop must beSome("test after empty")
    }
  }

}
