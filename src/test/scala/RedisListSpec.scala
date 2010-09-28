package net.fyrie.redis
package akka
package collection

import Commands._

import org.specs._
import specification.Context

class RedisListSpec extends Specification {
  implicit val r = new AkkaRedisClient("localhost", 16379)

  val empty = new Context {
    before {
      r !! flushdb
    }
    after {
      r !! flushdb
    }
  }

  "size" ->- empty should {
    "return 0 for empty list" in {
      val list = RedisList[String]("test list 1")
      list must haveSize(0)
    }
    "return proper values for filled lists" in {
      val list = RedisList[String]("test list 1")
      list += "Hello" += "World"
      list must haveSize(2)
      "I'm going first" +=: list
      list must haveSize(3)
    }
  }
  "slice" ->- empty should {
    "return a slice" in {
      val list = RedisList[String]("test list 1")
      "I'm going first" +=: list += ("Hello", "World")
      list.slice(1,-1) must_== Seq("Hello", "World")
      list.slice(0,1) must_== Seq("I'm going first", "Hello")
    }
  }
  "lpop/rpop" ->- empty should {
    "pop from the list" in {
      val list = RedisList[String]("test list 1")
      list += ("Hello", "World", "test1", "test2", "I'm last")
      "I'm going first" +=: list
      list must haveSize(6)
      list.lpop must_== ("I'm going first")
      list must haveSize(5)
      list.lpop must_== ("Hello")
      list must haveSize(4)
      list.rpop must_== ("I'm last")
      list must haveSize(3)
      list.rpop must_== ("test2")
      list must haveSize(2)
      list.lpop must_== ("World")
      list must haveSize(1)
      list.rpop must_== ("test1")
      list must haveSize(0)
      list.rpop must throwA[NoSuchElementException]
      list.lpush("test after exception")
      list.rpop must_== ("test after exception")
    }
  }

}
