package net.fyrie.redis

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers

class SetOperationsSpec extends Spec
  with ShouldMatchers
  with RedisTestServer {

  describe("sadd") {
    it("should add a non-existent value to the set") {
      r.sync.sadd("set-1", "foo") should equal(true)
      r.sync.sadd("set-1", "bar") should equal(true)
    }
    it("should not add an existing value to the set") {
      r.sync.sadd("set-1", "foo") should equal(true)
      r.sync.sadd("set-1", "foo") should equal(false)
    }
    it("should fail if the key points to a non-set") {
      r.sync.lpush("list-1", "foo") should equal(1)
      val thrown = evaluating { r.sync.sadd("list-1", "foo") } should produce[Exception]
      thrown.getMessage should equal("ERR Operation against a key holding the wrong kind of value")
    }
  }

  describe("srem") {
    it("should remove a value from the set") {
      r.sync.sadd("set-1", "foo") should equal(true)
      r.sync.sadd("set-1", "bar") should equal(true)
      r.sync.srem("set-1", "bar") should equal(true)
      r.sync.srem("set-1", "foo") should equal(true)
    }
    it("should not do anything if the value does not exist") {
      r.sync.sadd("set-1", "foo") should equal(true)
      r.sync.srem("set-1", "bar") should equal(false)
    }
    it("should fail if the key points to a non-set") {
      r.sync.lpush("list-1", "foo") should equal(1)
      val thrown = evaluating { r.sync.srem("list-1", "foo") } should produce[Exception]
      thrown.getMessage should equal("ERR Operation against a key holding the wrong kind of value")
    }
  }

  describe("spop") {
    it("should pop a random element") {
      r.sync.sadd("set-1", "foo") should equal(true)
      r.sync.sadd("set-1", "bar") should equal(true)
      r.sync.sadd("set-1", "baz") should equal(true)
      r.sync.spop("set-1").parse[String] should (equal(Some("foo")) or equal(Some("bar")) or equal(Some("baz")))
    }
    it("should return nil if the key does not exist") {
      r.sync.spop("set-1") should equal(None)
    }
  }

  describe("smove") {
    it("should move from one set to another") {
      r.sync.sadd("set-1", "foo") should equal(true)
      r.sync.sadd("set-1", "bar") should equal(true)
      r.sync.sadd("set-1", "baz") should equal(true)

      r.sync.sadd("set-2", "1") should equal(true)
      r.sync.sadd("set-2", "2") should equal(true)

      r.sync.smove("set-1", "set-2", "baz") should equal(true)
      r.sync.sadd("set-2", "baz") should equal(false)
      r.sync.sadd("set-1", "baz") should equal(true)
    }
    it("should return 0 if the element does not exist in source set") {
      r.sync.sadd("set-1", "foo") should equal(true)
      r.sync.sadd("set-1", "bar") should equal(true)
      r.sync.sadd("set-1", "baz") should equal(true)
      r.sync.smove("set-1", "set-2", "bat") should equal(false)
      r.sync.smove("set-3", "set-2", "bat") should equal(false)
    }
    it("should give error if the source or destination key is not a set") {
      r.sync.lpush("list-1", "foo") should equal(1)
      r.sync.lpush("list-1", "bar") should equal(2)
      r.sync.lpush("list-1", "baz") should equal(3)
      r.sync.sadd("set-1", "foo") should equal(true)
      val thrown = evaluating { r.sync.smove("list-1", "set-1", "bat") } should produce[Exception]
      thrown.getMessage should equal("ERR Operation against a key holding the wrong kind of value")
    }
  }

  describe("scard") {
    it("should return cardinality") {
      r.sync.sadd("set-1", "foo") should equal(true)
      r.sync.sadd("set-1", "bar") should equal(true)
      r.sync.sadd("set-1", "baz") should equal(true)
      r.sync.scard("set-1") should equal(3)
    }
    it("should return 0 if key does not exist") {
      r.sync.scard("set-1") should equal(0)
    }
  }

  describe("sismember") {
    it("should return true for membership") {
      r.sync.sadd("set-1", "foo") should equal(true)
      r.sync.sadd("set-1", "bar") should equal(true)
      r.sync.sadd("set-1", "baz") should equal(true)
      r.sync.sismember("set-1", "foo") should equal(true)
    }
    it("should return false for no membership") {
      r.sync.sadd("set-1", "foo") should equal(true)
      r.sync.sadd("set-1", "bar") should equal(true)
      r.sync.sadd("set-1", "baz") should equal(true)
      r.sync.sismember("set-1", "fo") should equal(false)
    }
    it("should return false if key does not exist") {
      r.sync.sismember("set-1", "fo") should equal(false)
    }
  }

  describe("sinter") {
    it("should return intersection") {
      r.sync.sadd("set-1", "foo") should equal(true)
      r.sync.sadd("set-1", "bar") should equal(true)
      r.sync.sadd("set-1", "baz") should equal(true)

      r.sync.sadd("set-2", "foo") should equal(true)
      r.sync.sadd("set-2", "bat") should equal(true)
      r.sync.sadd("set-2", "baz") should equal(true)

      r.sync.sadd("set-3", "for") should equal(true)
      r.sync.sadd("set-3", "bat") should equal(true)
      r.sync.sadd("set-3", "bay") should equal(true)

      r.sync.sinter(Set("set-1", "set-2")).parse[String] should equal(Set("foo", "baz"))
      r.sync.sinter(Set("set-1", "set-3")).parse[String] should equal(Set.empty)
    }
    it("should return empty set for non-existing key") {
      r.sync.sadd("set-1", "foo") should equal(true)
      r.sync.sadd("set-1", "bar") should equal(true)
      r.sync.sadd("set-1", "baz") should equal(true)
      r.sync.sinter(Set("set-1", "set-4")).parse[String] should equal(Set.empty)
    }
  }

  describe("sinterstore") {
    it("should store intersection") {
      r.sync.sadd("set-1", "foo") should equal(true)
      r.sync.sadd("set-1", "bar") should equal(true)
      r.sync.sadd("set-1", "baz") should equal(true)

      r.sync.sadd("set-2", "foo") should equal(true)
      r.sync.sadd("set-2", "bat") should equal(true)
      r.sync.sadd("set-2", "baz") should equal(true)

      r.sync.sadd("set-3", "for") should equal(true)
      r.sync.sadd("set-3", "bat") should equal(true)
      r.sync.sadd("set-3", "bay") should equal(true)

      r.sync.sinterstore("set-r", Set("set-1", "set-2")) should equal(2)
      r.sync.scard("set-r") should equal(2)
      r.sync.sinterstore("set-s", Set("set-1", "set-3")) should equal(0)
      r.sync.scard("set-s") should equal(0)
    }
    it("should return empty set for non-existing key") {
      r.sync.sadd("set-1", "foo") should equal(true)
      r.sync.sadd("set-1", "bar") should equal(true)
      r.sync.sadd("set-1", "baz") should equal(true)
      r.sync.sinterstore("set-r", Seq("set-1", "set-4")) should equal(0)
      r.sync.scard("set-r") should equal(0)
    }
  }

  describe("sunion") {
    it("should return union") {
      r.sync.sadd("set-1", "foo") should equal(true)
      r.sync.sadd("set-1", "bar") should equal(true)
      r.sync.sadd("set-1", "baz") should equal(true)

      r.sync.sadd("set-2", "foo") should equal(true)
      r.sync.sadd("set-2", "bat") should equal(true)
      r.sync.sadd("set-2", "baz") should equal(true)

      r.sync.sadd("set-3", "for") should equal(true)
      r.sync.sadd("set-3", "bat") should equal(true)
      r.sync.sadd("set-3", "bay") should equal(true)

      r.sync.sunion(Set("set-1", "set-2")).parse[String] should equal(Set("foo", "bar", "baz", "bat"))
      r.sync.sunion(Set("set-1", "set-3")).parse[String] should equal(Set("foo", "bar", "baz", "for", "bat", "bay"))
    }
    it("should return empty set for non-existing key") {
      r.sync.sadd("set-1", "foo") should equal(true)
      r.sync.sadd("set-1", "bar") should equal(true)
      r.sync.sadd("set-1", "baz") should equal(true)
      r.sync.sunion(Seq("set-1", "set-2")).parse[String] should equal(Set("foo", "bar", "baz"))
    }
  }

  describe("sunionstore") {
    it("should store union") {
      r.sync.sadd("set-1", "foo") should equal(true)
      r.sync.sadd("set-1", "bar") should equal(true)
      r.sync.sadd("set-1", "baz") should equal(true)

      r.sync.sadd("set-2", "foo") should equal(true)
      r.sync.sadd("set-2", "bat") should equal(true)
      r.sync.sadd("set-2", "baz") should equal(true)

      r.sync.sadd("set-3", "for") should equal(true)
      r.sync.sadd("set-3", "bat") should equal(true)
      r.sync.sadd("set-3", "bay") should equal(true)

      r.sync.sunionstore("set-r", Set("set-1", "set-2")) should equal(4)
      r.sync.scard("set-r") should equal(4)
      r.sync.sunionstore("set-s", Set("set-1", "set-3")) should equal(6)
      r.sync.scard("set-s") should equal(6)
    }
    it("should treat non-existing keys as empty sets") {
      r.sync.sadd("set-1", "foo") should equal(true)
      r.sync.sadd("set-1", "bar") should equal(true)
      r.sync.sadd("set-1", "baz") should equal(true)
      r.sync.sunionstore("set-r", Set("set-1", "set-4")) should equal(3)
      r.sync.scard("set-r") should equal(3)
    }
  }

  describe("sdiff") {
    it("should return diff") {
      r.sync.sadd("set-1", "foo") should equal(true)
      r.sync.sadd("set-1", "bar") should equal(true)
      r.sync.sadd("set-1", "baz") should equal(true)

      r.sync.sadd("set-2", "foo") should equal(true)
      r.sync.sadd("set-2", "bat") should equal(true)
      r.sync.sadd("set-2", "baz") should equal(true)

      r.sync.sadd("set-3", "for") should equal(true)
      r.sync.sadd("set-3", "bat") should equal(true)
      r.sync.sadd("set-3", "bay") should equal(true)

      r.sync.sdiff("set-1", Set("set-2", "set-3")).parse[String] should equal(Set("bar"))
    }
    it("should treat non-existing keys as empty sets") {
      r.sync.sadd("set-1", "foo") should equal(true)
      r.sync.sadd("set-1", "bar") should equal(true)
      r.sync.sadd("set-1", "baz") should equal(true)
      r.sync.sdiff("set-1", Set("set-2")).parse[String] should equal(Set("foo", "bar", "baz"))
    }
  }

  describe("smembers") {
    it("should return members of a set") {
      r.sync.sadd("set-1", "foo") should equal(true)
      r.sync.sadd("set-1", "bar") should equal(true)
      r.sync.sadd("set-1", "baz") should equal(true)
      r.sync.smembers("set-1").parse[String] should equal(Set("foo", "bar", "baz"))
    }
    it("should return None for an empty set") {
      r.sync.smembers("set-1").parse[String] should equal(Set.empty)
    }
  }

  describe("srandmember") {
    it("should return a random member") {
      r.sync.sadd("set-1", "foo") should equal(true)
      r.sync.sadd("set-1", "bar") should equal(true)
      r.sync.sadd("set-1", "baz") should equal(true)
      r.sync.srandmember("set-1").parse[String] should (equal(Some("foo")) or equal(Some("bar")) or equal(Some("baz")))
    }
    it("should return None for a non-existing key") {
      r.sync.srandmember("set-1").parse[String] should equal(None)
    }
  }
}
