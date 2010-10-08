package net.fyrie.redis

import Commands._

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith


@RunWith(classOf[JUnitRunner])
class ListOperationsSpec extends Spec 
                         with ShouldMatchers
                         with RedisTestServer {

  describe("lpush") {
    it("should add to the head of the list") {
      r send lpush("list-1", "foo") should equal(1)
      r send lpush("list-1", "bar") should equal(2)
    }
    it("should throw if the key has a non-list value") {
      r send set("anshin-1", "debasish")
      val thrown = evaluating { r send lpush("anshin-1", "bar") } should produce [Exception]
      thrown.getMessage should equal("ERR Operation against a key holding the wrong kind of value")
    }
  }

  describe("rpush") {
    it("should add to the head of the list") {
      r send rpush("list-1", "foo") should equal(1)
      r send rpush("list-1", "bar") should equal(2)
    }
    it("should throw if the key has a non-list value") {
      r send set("anshin-1", "debasish")
      val thrown = evaluating { r send rpush("anshin-1", "bar") } should produce [Exception]
      thrown.getMessage should equal("ERR Operation against a key holding the wrong kind of value")
    }
  }

  describe("llen") {
    it("should return the length of the list") {
      r send lpush("list-1", "foo") should equal(1)
      r send lpush("list-1", "bar") should equal(2)
      r send llen("list-1") should equal(2)
    }
    it("should return 0 for a non-existent key") {
      r send llen("list-2") should equal(0)
    }
    it("should throw for a non-list key") {
      r send set("anshin-1", "debasish")
      val thrown = evaluating { r send llen("anshin-1") } should produce [Exception]
      thrown.getMessage should equal("ERR Operation against a key holding the wrong kind of value")
    }
  }

  describe("lrange") {
    it("should return the range") {
      r send lpush("list-1", "6") should equal(1)
      r send lpush("list-1", "5") should equal(2)
      r send lpush("list-1", "4") should equal(3)
      r send lpush("list-1", "3") should equal(4)
      r send lpush("list-1", "2") should equal(5)
      r send lpush("list-1", "1") should equal(6)
      r send llen("list-1") should equal(6)
      r send lrange("list-1", 0, 4) should equal(Result(List("1", "2", "3", "4", "5")))
    }
    it("should return empty list if start > end") {
      r send lpush("list-1", "3") should equal(1)
      r send lpush("list-1", "2") should equal(2)
      r send lpush("list-1", "1") should equal(3)
      r send lrange("list-1", 2, 0) should equal(Result(Nil))
    }
    it("should treat as end of list if end is over the actual end of list") {
      r send lpush("list-1", "3") should equal(1)
      r send lpush("list-1", "2") should equal(2)
      r send lpush("list-1", "1") should equal(3)
      r send lrange("list-1", 0, 7) should equal(Result(List("1", "2", "3")))
    }
  }

  describe("ltrim") {
    it("should trim to the input size") {
      r send lpush("list-1", "6") should equal(1)
      r send lpush("list-1", "5") should equal(2)
      r send lpush("list-1", "4") should equal(3)
      r send lpush("list-1", "3") should equal(4)
      r send lpush("list-1", "2") should equal(5)
      r send lpush("list-1", "1") should equal(6)
      r send ltrim("list-1", 0, 3)
      r send llen("list-1") should equal(4)
    }
    it("should should return empty list for start > end") {
      r send lpush("list-1", "6") should equal(1)
      r send lpush("list-1", "5") should equal(2)
      r send lpush("list-1", "4") should equal(3)
      r send ltrim("list-1", 6, 3)
      r send llen("list-1") should equal(0)
    }
    it("should treat as end of list if end is over the actual end of list") {
      r send lpush("list-1", "6") should equal(1)
      r send lpush("list-1", "5") should equal(2)
      r send lpush("list-1", "4") should equal(3)
      r send ltrim("list-1", 0, 12)
      r send llen("list-1") should equal(3)
    }
  }

  describe("lindex") {
    it("should return the value at index") {
      r send lpush("list-1", "6") should equal(1)
      r send lpush("list-1", "5") should equal(2)
      r send lpush("list-1", "4") should equal(3)
      r send lpush("list-1", "3") should equal(4)
      r send lpush("list-1", "2") should equal(5)
      r send lpush("list-1", "1") should equal(6)
      r send lindex("list-1", 2) should equal(Result("3"))
      r send lindex("list-1", 3) should equal(Result("4"))
      r send lindex("list-1", -1) should equal(Result("6"))
    }
    it("should return None if the key does not point to a list") {
      r send set("anshin-1", "debasish")
      r send lindex("list-1", 0) should equal(NoResult)
    }
    it("should return empty string for an index out of range") {
      r send lpush("list-1", "6") should equal(1)
      r send lpush("list-1", "5") should equal(2)
      r send lpush("list-1", "4") should equal(3)
      r send lindex("list-1", 8) should equal(NoResult) // the protocol says it will return empty string
    }
  }

  describe("lset") {
    it("should set value for key at index") {
      r send lpush("list-1", "6") should equal(1)
      r send lpush("list-1", "5") should equal(2)
      r send lpush("list-1", "4") should equal(3)
      r send lpush("list-1", "3") should equal(4)
      r send lpush("list-1", "2") should equal(5)
      r send lpush("list-1", "1") should equal(6)
      r send lset("list-1", 2, "30")
      r send lindex("list-1", 2) should equal(Result("30"))
    }
    it("should generate error for out of range index") {
      r send lpush("list-1", "6") should equal(1)
      r send lpush("list-1", "5") should equal(2)
      r send lpush("list-1", "4") should equal(3)
      val thrown = evaluating { r send lset("list-1", 12, "30") } should produce [Exception]
      thrown.getMessage should equal("ERR index out of range")
    }
  }

  describe("lrem") {
    it("should remove count elements matching value from beginning") {
      r send lpush("list-1", "6") should equal(1)
      r send lpush("list-1", "hello") should equal(2)
      r send lpush("list-1", "4") should equal(3)
      r send lpush("list-1", "hello") should equal(4)
      r send lpush("list-1", "hello") should equal(5)
      r send lpush("list-1", "hello") should equal(6)
      r send lrem("list-1", 2, "hello") should equal(2)
      r send llen("list-1") should equal(4)
    }
    it("should remove all elements matching value from beginning") {
      r send lpush("list-1", "6") should equal(1)
      r send lpush("list-1", "hello") should equal(2)
      r send lpush("list-1", "4") should equal(3)
      r send lpush("list-1", "hello") should equal(4)
      r send lpush("list-1", "hello") should equal(5)
      r send lpush("list-1", "hello") should equal(6)
      r send lrem("list-1", 0, "hello") should equal(4)
      r send llen("list-1") should equal(2)
    }
    it("should remove count elements matching value from end") {
      r send lpush("list-1", "6") should equal(1)
      r send lpush("list-1", "hello") should equal(2)
      r send lpush("list-1", "4") should equal(3)
      r send lpush("list-1", "hello") should equal(4)
      r send lpush("list-1", "hello") should equal(5)
      r send lpush("list-1", "hello") should equal(6)
      r send lrem("list-1", -2, "hello") should equal(2)
      r send llen("list-1") should equal(4)
      r send lindex("list-1", -2) should equal(Result("4"))
    }
  }

  describe("lpop") {
    it("should pop the first one from head") {
      r send lpush("list-1", "6") should equal(1)
      r send lpush("list-1", "5") should equal(2)
      r send lpush("list-1", "4") should equal(3)
      r send lpush("list-1", "3") should equal(4)
      r send lpush("list-1", "2") should equal(5)
      r send lpush("list-1", "1") should equal(6)
      r send lpop("list-1") should equal(Result("1"))
      r send lpop("list-1") should equal(Result("2"))
      r send lpop("list-1") should equal(Result("3"))
      r send llen("list-1") should equal(3)
    }
    it("should give nil for non-existent key") {
      r send lpush("list-1", "6") should equal(1)
      r send lpush("list-1", "5") should equal(2)
      r send lpop("list-2") should equal(NoResult)
      r send llen("list-1") should equal(2)
    }
  }

  describe("rpop") {
    it("should pop the first one from tail") {
      r send lpush("list-1", "6") should equal(1)
      r send lpush("list-1", "5") should equal(2)
      r send lpush("list-1", "4") should equal(3)
      r send lpush("list-1", "3") should equal(4)
      r send lpush("list-1", "2") should equal(5)
      r send lpush("list-1", "1") should equal(6)
      r send rpop("list-1") should equal(Result("6"))
      r send rpop("list-1") should equal(Result("5"))
      r send rpop("list-1") should equal(Result("4"))
      r send llen("list-1") should equal(3)
    }
    it("should give nil for non-existent key") {
      r send lpush("list-1", "6") should equal(1)
      r send lpush("list-1", "5") should equal(2)
      r send rpop("list-2") should equal(NoResult)
      r send llen("list-1") should equal(2)
    }
  }

  describe("rpoplpush") {
    it("should do") {
      r send rpush("list-1", "a") should equal(1)
      r send rpush("list-1", "b") should equal(2)
      r send rpush("list-1", "c") should equal(3)

      r send rpush("list-2", "foo") should equal(1)
      r send rpush("list-2", "bar") should equal(2)
      r send rpoplpush("list-1", "list-2") should equal(Result("c"))
      r send lindex("list-2", 0) should equal(Result("c"))
      r send llen("list-1") should equal(2)
      r send llen("list-2") should equal(3)
    }

    it("should rotate the list when src and dest are the same") {
      r send rpush("list-1", "a") should equal(1)
      r send rpush("list-1", "b") should equal(2)
      r send rpush("list-1", "c") should equal(3)
      r send rpoplpush("list-1", "list-1") should equal(Result("c"))
      r send lindex("list-1", 0) should equal(Result("c"))
      r send lindex("list-1", 2) should equal(Result("b"))
      r send llen("list-1") should equal(3)
    }

    it("should give None for non-existent key") {
      r send rpoplpush("list-1", "list-2") should equal(NoResult)
      r send rpush("list-1", "a") should equal(1)
      r send rpush("list-1", "b") should equal(2)
      r send rpoplpush("list-1", "list-2") should equal(Result("b"))
    }
  }

  describe("lpush with newlines in strings") {
    it("should add to the head of the list") {
      r send lpush("list-1", "foo\nbar\nbaz") should equal(1)
      r send lpush("list-1", "bar\nfoo\nbaz") should equal(2)
      r send lpop("list-1") should equal(Result("bar\nfoo\nbaz"))
      r send lpop("list-1") should equal(Result("foo\nbar\nbaz"))
    }
  }

  /**
  describe("blpop") {
    it ("should do") {
      r send lpush("l1", "a") should equal(true)
      r send lpop("l1")
      r send llen("l1") should equal(Some(0))
    }
  }
**/
}

