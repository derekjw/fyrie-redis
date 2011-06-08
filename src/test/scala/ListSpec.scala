package net.fyrie.redis

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers

class ListOperationsSpec extends Spec 
                           with ShouldMatchers
                           with RedisTestServer {

  describe("lpush") {
    it("should add to the head of the list") {
      r.sync.lpush("list-1", "foo") should equal(1)
      r.sync.lpush("list-1", "bar") should equal(2)
    }
    it("should throw if the key has a non-list value") {
      r.set("anshin-1", "debasish")
      val thrown = evaluating { r.sync.lpush("anshin-1", "bar") } should produce [Exception]
      thrown.getMessage should equal("ERR Operation against a key holding the wrong kind of value")
    }
  }

  describe("rpush") {
    it("should add to the head of the list") {
      r.sync.rpush("list-1", "foo") should equal(1)
      r.sync.rpush("list-1", "bar") should equal(2)
    }
    it("should throw if the key has a non-list value") {
      r.set("anshin-1", "debasish")
      val thrown = evaluating { r.sync.rpush("anshin-1", "bar") } should produce [Exception]
      thrown.getMessage should equal("ERR Operation against a key holding the wrong kind of value")
    }
  }

  describe("llen") {
    it("should return the length of the list") {
      r.sync.lpush("list-1", "foo") should equal(1)
      r.sync.lpush("list-1", "bar") should equal(2)
      r.sync.llen("list-1") should equal(2)
    }
    it("should return 0 for a non-existent key") {
      r.sync.llen("list-2") should equal(0)
    }
    it("should throw for a non-list key") {
      r.set("anshin-1", "debasish")
      val thrown = evaluating { r.sync.llen("anshin-1") } should produce [Exception]
      thrown.getMessage should equal("ERR Operation against a key holding the wrong kind of value")
    }
  }

  describe("lrange") {
    it("should return the range") {
      r.sync.lpush("list-1", "6") should equal(1)
      r.sync.lpush("list-1", "5") should equal(2)
      r.sync.lpush("list-1", "4") should equal(3)
      r.sync.lpush("list-1", "3") should equal(4)
      r.sync.lpush("list-1", "2") should equal(5)
      r.sync.lpush("list-1", "1") should equal(6)
      r.sync.llen("list-1") should equal(6)
      r.sync.lrange("list-1", 0, 4).parse[String] should equal(Some(List("1", "2", "3", "4", "5")))
    }
    it("should return empty list if start > end") {
      r.sync.lpush("list-1", "3") should equal(1)
      r.sync.lpush("list-1", "2") should equal(2)
      r.sync.lpush("list-1", "1") should equal(3)
      r.sync.lrange("list-1", 2, 0).parse[String] should equal(Some(Nil))
    }
    it("should treat as end of list if end is over the actual end of list") {
      r.sync.lpush("list-1", "3") should equal(1)
      r.sync.lpush("list-1", "2") should equal(2)
      r.sync.lpush("list-1", "1") should equal(3)
      r.sync.lrange("list-1", 0, 7).parse[String] should equal(Some(List("1", "2", "3")))
    }
  }

  describe("ltrim") {
    it("should trim to the input size") {
      r.sync.lpush("list-1", "6") should equal(1)
      r.sync.lpush("list-1", "5") should equal(2)
      r.sync.lpush("list-1", "4") should equal(3)
      r.sync.lpush("list-1", "3") should equal(4)
      r.sync.lpush("list-1", "2") should equal(5)
      r.sync.lpush("list-1", "1") should equal(6)
      r.ltrim("list-1", 0, 3)
      r.sync.llen("list-1") should equal(4)
    }
    it("should should return empty list for start > end") {
      r.sync.lpush("list-1", "6") should equal(1)
      r.sync.lpush("list-1", "5") should equal(2)
      r.sync.lpush("list-1", "4") should equal(3)
      r.ltrim("list-1", 6, 3)
      r.sync.llen("list-1") should equal(0)
    }
    it("should treat as end of list if end is over the actual end of list") {
      r.sync.lpush("list-1", "6") should equal(1)
      r.sync.lpush("list-1", "5") should equal(2)
      r.sync.lpush("list-1", "4") should equal(3)
      r.ltrim("list-1", 0, 12)
      r.sync.llen("list-1") should equal(3)
    }
  }

  describe("lindex") {
    it("should return the value at index") {
      r.sync.lpush("list-1", "6") should equal(1)
      r.sync.lpush("list-1", "5") should equal(2)
      r.sync.lpush("list-1", "4") should equal(3)
      r.sync.lpush("list-1", "3") should equal(4)
      r.sync.lpush("list-1", "2") should equal(5)
      r.sync.lpush("list-1", "1") should equal(6)
      r.sync.lindex("list-1", 2).parse[String] should equal(Some("3"))
      r.sync.lindex("list-1", 3).parse[String] should equal(Some("4"))
      r.sync.lindex("list-1", -1).parse[String] should equal(Some("6"))
    }
    it("should return None if the key does not point to a list") {
      r.set("anshin-1", "debasish")
      r.sync.lindex("list-1", 0).parse[String] should equal(None)
    }
    it("should return empty string for an index out of range") {
      r.sync.lpush("list-1", "6") should equal(1)
      r.sync.lpush("list-1", "5") should equal(2)
      r.sync.lpush("list-1", "4") should equal(3)
      r.sync.lindex("list-1", 8).parse[String] should equal(None) // the protocol says it will return empty string
    }
  }

  describe("lset") {
    it("should set value for key at index") {
      r.sync.lpush("list-1", "6") should equal(1)
      r.sync.lpush("list-1", "5") should equal(2)
      r.sync.lpush("list-1", "4") should equal(3)
      r.sync.lpush("list-1", "3") should equal(4)
      r.sync.lpush("list-1", "2") should equal(5)
      r.sync.lpush("list-1", "1") should equal(6)
      r.lset("list-1", 2, "30")
      r.sync.lindex("list-1", 2).parse[String] should equal(Some("30"))
    }
    it("should generate error for out of range index") {
      r.sync.lpush("list-1", "6") should equal(1)
      r.sync.lpush("list-1", "5") should equal(2)
      r.sync.lpush("list-1", "4") should equal(3)
      val thrown = evaluating { r.sync.lset("list-1", 12, "30") } should produce [Exception]
      thrown.getMessage should equal("ERR index out of range")
    }
  }

  describe("lrem") {
    it("should remove count elements matching value from beginning") {
      r.sync.lpush("list-1", "6") should equal(1)
      r.sync.lpush("list-1", "hello") should equal(2)
      r.sync.lpush("list-1", "4") should equal(3)
      r.sync.lpush("list-1", "hello") should equal(4)
      r.sync.lpush("list-1", "hello") should equal(5)
      r.sync.lpush("list-1", "hello") should equal(6)
      r.sync.lrem("list-1", "hello", 2) should equal(2)
      r.sync.llen("list-1") should equal(4)
    }
    it("should remove all elements matching value from beginning") {
      r.sync.lpush("list-1", "6") should equal(1)
      r.sync.lpush("list-1", "hello") should equal(2)
      r.sync.lpush("list-1", "4") should equal(3)
      r.sync.lpush("list-1", "hello") should equal(4)
      r.sync.lpush("list-1", "hello") should equal(5)
      r.sync.lpush("list-1", "hello") should equal(6)
      r.sync.lrem("list-1", "hello") should equal(4)
      r.sync.llen("list-1") should equal(2)
    }
    it("should remove count elements matching value from end") {
      r.sync.lpush("list-1", "6") should equal(1)
      r.sync.lpush("list-1", "hello") should equal(2)
      r.sync.lpush("list-1", "4") should equal(3)
      r.sync.lpush("list-1", "hello") should equal(4)
      r.sync.lpush("list-1", "hello") should equal(5)
      r.sync.lpush("list-1", "hello") should equal(6)
      r.sync.lrem("list-1", "hello", -2) should equal(2)
      r.sync.llen("list-1") should equal(4)
      r.sync.lindex("list-1", -2).parse[String] should equal(Some("4"))
    }
  }

  describe("lpop") {
    it("should pop the first one from head") {
      r.sync.lpush("list-1", "6") should equal(1)
      r.sync.lpush("list-1", "5") should equal(2)
      r.sync.lpush("list-1", "4") should equal(3)
      r.sync.lpush("list-1", "3") should equal(4)
      r.sync.lpush("list-1", "2") should equal(5)
      r.sync.lpush("list-1", "1") should equal(6)
      r.sync.lpop("list-1").parse[String] should equal(Some("1"))
      r.sync.lpop("list-1").parse[String] should equal(Some("2"))
      r.sync.lpop("list-1").parse[String] should equal(Some("3"))
      r.sync.llen("list-1") should equal(3)
    }
    it("should give nil for non-existent key") {
      r.sync.lpush("list-1", "6") should equal(1)
      r.sync.lpush("list-1", "5") should equal(2)
      r.sync.lpop("list-2").parse[String] should equal(None)
      r.sync.llen("list-1") should equal(2)
    }
  }

  describe("rpop") {
    it("should pop the first one from tail") {
      r.sync.lpush("list-1", "6") should equal(1)
      r.sync.lpush("list-1", "5") should equal(2)
      r.sync.lpush("list-1", "4") should equal(3)
      r.sync.lpush("list-1", "3") should equal(4)
      r.sync.lpush("list-1", "2") should equal(5)
      r.sync.lpush("list-1", "1") should equal(6)
      r.sync.rpop("list-1").parse[String] should equal(Some("6"))
      r.sync.rpop("list-1").parse[String] should equal(Some("5"))
      r.sync.rpop("list-1").parse[String] should equal(Some("4"))
      r.sync.llen("list-1") should equal(3)
    }
    it("should give nil for non-existent key") {
      r.sync.lpush("list-1", "6") should equal(1)
      r.sync.lpush("list-1", "5") should equal(2)
      r.sync.rpop("list-2").parse[String] should equal(None)
      r.sync.llen("list-1") should equal(2)
    }
  }

  describe("rpoplpush") {
    it("should do") {
      r.sync.rpush("list-1", "a") should equal(1)
      r.sync.rpush("list-1", "b") should equal(2)
      r.sync.rpush("list-1", "c") should equal(3)
      r.sync.rpush("list-2", "foo") should equal(1)
      r.sync.rpush("list-2", "bar") should equal(2)
      r.sync.rpoplpush("list-1", "list-2").parse[String] should equal(Some("c"))
      r.sync.lindex("list-2", 0).parse[String] should equal(Some("c"))
      r.sync.llen("list-1") should equal(2)
      r.sync.llen("list-2") should equal(3)
    }

    it("should rotate the list when src and dest are the same") {
      r.sync.rpush("list-1", "a") should equal(1)
      r.sync.rpush("list-1", "b") should equal(2)
      r.sync.rpush("list-1", "c") should equal(3)
      r.sync.rpoplpush("list-1", "list-1").parse[String] should equal(Some("c"))
      r.sync.lindex("list-1", 0).parse[String] should equal(Some("c"))
      r.sync.lindex("list-1", 2).parse[String] should equal(Some("b"))
      r.sync.llen("list-1") should equal(3)
    }

    it("should give None for non-existent key") {
      r.sync.rpoplpush("list-1", "list-2").parse[String] should equal(None)
      r.sync.rpush("list-1", "a") should equal(1)
      r.sync.rpush("list-1", "b") should equal(2)
      r.sync.rpoplpush("list-1", "list-2").parse[String] should equal(Some("b"))
    }
  }

  describe("lpush with newlines in strings") {
    it("should add to the head of the list") {
      r.sync.lpush("list-1", "foo\nbar\nbaz") should equal(1)
      r.sync.lpush("list-1", "bar\nfoo\nbaz") should equal(2)
      r.sync.lpop("list-1").parse[String] should equal(Some("bar\nfoo\nbaz"))
      r.sync.lpop("list-1").parse[String] should equal(Some("foo\nbar\nbaz"))
    }
  }

  /**
  describe("blpop") {
    it ("should do") {
      r.sync.lpush("l1", "a") should equal(true)
      r.sync.lpop("l1")
      r.sync.llen("l1") should equal(Some(0))
    }
  }
**/
}

