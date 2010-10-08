package net.fyrie.redis

import Commands._

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith


@RunWith(classOf[JUnitRunner])
class SetOperationsSpec extends Spec 
                        with ShouldMatchers
                        with RedisTestServer {

  describe("sadd") {
    it("should add a non-existent value to the set") {
      r send sadd("set-1", "foo") should equal(true)
      r send sadd("set-1", "bar") should equal(true)
    }
    it("should not add an existing value to the set") {
      r send sadd("set-1", "foo") should equal(true)
      r send sadd("set-1", "foo") should equal(false)
    }
    it("should fail if the key points to a non-set") {
      r send lpush("list-1", "foo") should equal(1)
      val thrown = evaluating { r send sadd("list-1", "foo") } should produce [Exception]
      thrown.getMessage should equal("ERR Operation against a key holding the wrong kind of value")
    }
  }

  describe("srem") {
    it("should remove a value from the set") {
      r send sadd("set-1", "foo") should equal(true)
      r send sadd("set-1", "bar") should equal(true)
      r send srem("set-1", "bar") should equal(true)
      r send srem("set-1", "foo") should equal(true)
    }
    it("should not do anything if the value does not exist") {
      r send sadd("set-1", "foo") should equal(true)
      r send srem("set-1", "bar") should equal(false)
    }
    it("should fail if the key points to a non-set") {
      r send lpush("list-1", "foo") should equal(1)
      val thrown = evaluating { r send srem("list-1", "foo") } should produce [Exception]
      thrown.getMessage should equal("ERR Operation against a key holding the wrong kind of value")
    }
  }

  describe("spop") {
    it("should pop a random element") {
      r send sadd("set-1", "foo") should equal(true)
      r send sadd("set-1", "bar") should equal(true)
      r send sadd("set-1", "baz") should equal(true)
      r send spop("set-1") should (equal(Result("foo")) or equal(Result("bar")) or equal(Result("baz")))
    }
    it("should return nil if the key does not exist") {
      r send spop("set-1") should equal(NoResult)
    }
  }

  describe("smove") {
    it("should move from one set to another") {
      r send sadd("set-1", "foo") should equal(true)
      r send sadd("set-1", "bar") should equal(true)
      r send sadd("set-1", "baz") should equal(true)

      r send sadd("set-2", "1") should equal(true)
      r send sadd("set-2", "2") should equal(true)

      r send smove("set-1", "set-2", "baz") should equal(true)
      r send sadd("set-2", "baz") should equal(false)
      r send sadd("set-1", "baz") should equal(true)
    }
    it("should return 0 if the element does not exist in source set") {
      r send sadd("set-1", "foo") should equal(true)
      r send sadd("set-1", "bar") should equal(true)
      r send sadd("set-1", "baz") should equal(true)
      r send smove("set-1", "set-2", "bat") should equal(false)
      r send smove("set-3", "set-2", "bat") should equal(false)
    }
    it("should give error if the source or destination key is not a set") {
      r send lpush("list-1", "foo") should equal(1)
      r send lpush("list-1", "bar") should equal(2)
      r send lpush("list-1", "baz") should equal(3)
      r send sadd("set-1", "foo") should equal(true)
      val thrown = evaluating { r send smove("list-1", "set-1", "bat") } should produce [Exception]
      thrown.getMessage should equal("ERR Operation against a key holding the wrong kind of value")
    }
  }

  describe("scard") {
    it("should return cardinality") {
      r send sadd("set-1", "foo") should equal(true)
      r send sadd("set-1", "bar") should equal(true)
      r send sadd("set-1", "baz") should equal(true)
      r send scard("set-1") should equal(3)
    }
    it("should return 0 if key does not exist") {
      r send scard("set-1") should equal(0)
    }
  }

  describe("sismember") {
    it("should return true for membership") {
      r send sadd("set-1", "foo") should equal(true)
      r send sadd("set-1", "bar") should equal(true)
      r send sadd("set-1", "baz") should equal(true)
      r send sismember("set-1", "foo") should equal(true)
    }
    it("should return false for no membership") {
      r send sadd("set-1", "foo") should equal(true)
      r send sadd("set-1", "bar") should equal(true)
      r send sadd("set-1", "baz") should equal(true)
      r send sismember("set-1", "fo") should equal(false)
    }
    it("should return false if key does not exist") {
      r send sismember("set-1", "fo") should equal(false)
    }
  }

  describe("sinter") {
    it("should return intersection") {
      r send sadd("set-1", "foo") should equal(true)
      r send sadd("set-1", "bar") should equal(true)
      r send sadd("set-1", "baz") should equal(true)

      r send sadd("set-2", "foo") should equal(true)
      r send sadd("set-2", "bat") should equal(true)
      r send sadd("set-2", "baz") should equal(true)

      r send sadd("set-3", "for") should equal(true)
      r send sadd("set-3", "bat") should equal(true)
      r send sadd("set-3", "bay") should equal(true)

      r send sinter(Seq("set-1", "set-2")) map (_.toSet) should equal(Result(Set("foo", "baz")))
      r send sinter(Seq("set-1", "set-3")) map (_.toSet) should equal(Result(Set.empty))
    }
    it("should return empty set for non-existing key") {
      r send sadd("set-1", "foo") should equal(true)
      r send sadd("set-1", "bar") should equal(true)
      r send sadd("set-1", "baz") should equal(true)
      r send sinter(Seq("set-1", "set-4")) map (_.toSet) should equal(Result(Set())) 
    }
  }

  describe("sinterstore") {
    it("should store intersection") {
      r send sadd("set-1", "foo") should equal(true)
      r send sadd("set-1", "bar") should equal(true)
      r send sadd("set-1", "baz") should equal(true)

      r send sadd("set-2", "foo") should equal(true)
      r send sadd("set-2", "bat") should equal(true)
      r send sadd("set-2", "baz") should equal(true)

      r send sadd("set-3", "for") should equal(true)
      r send sadd("set-3", "bat") should equal(true)
      r send sadd("set-3", "bay") should equal(true)

      r send sinterstore("set-r", Seq("set-1", "set-2")) should equal(2)
      r send scard("set-r") should equal(2)
      r send sinterstore("set-s", Seq("set-1", "set-3")) should equal(0)
      r send scard("set-s") should equal(0)
    }
    it("should return empty set for non-existing key") {
      r send sadd("set-1", "foo") should equal(true)
      r send sadd("set-1", "bar") should equal(true)
      r send sadd("set-1", "baz") should equal(true)
      r send sinterstore("set-r", Seq("set-1", "set-4")) should equal(0)
      r send scard("set-r") should equal(0)
    }
  }

  describe("sunion") {
    it("should return union") {
      r send sadd("set-1", "foo") should equal(true)
      r send sadd("set-1", "bar") should equal(true)
      r send sadd("set-1", "baz") should equal(true)

      r send sadd("set-2", "foo") should equal(true)
      r send sadd("set-2", "bat") should equal(true)
      r send sadd("set-2", "baz") should equal(true)

      r send sadd("set-3", "for") should equal(true)
      r send sadd("set-3", "bat") should equal(true)
      r send sadd("set-3", "bay") should equal(true)

      r send sunion(Seq("set-1", "set-2")) map (_.toSet) should equal(Result(Set("foo", "bar", "baz", "bat")))
      r send sunion(Seq("set-1", "set-3")) map (_.toSet) should equal(Result(Set("foo", "bar", "baz", "for", "bat", "bay")))
    }
    it("should return empty set for non-existing key") {
      r send sadd("set-1", "foo") should equal(true)
      r send sadd("set-1", "bar") should equal(true)
      r send sadd("set-1", "baz") should equal(true)
      r send sunion(Seq("set-1", "set-2")) map (_.toSet) should equal(Result(Set("foo", "bar", "baz")))
    }
  }

  describe("sunionstore") {
    it("should store union") {
      r send sadd("set-1", "foo") should equal(true)
      r send sadd("set-1", "bar") should equal(true)
      r send sadd("set-1", "baz") should equal(true)

      r send sadd("set-2", "foo") should equal(true)
      r send sadd("set-2", "bat") should equal(true)
      r send sadd("set-2", "baz") should equal(true)

      r send sadd("set-3", "for") should equal(true)
      r send sadd("set-3", "bat") should equal(true)
      r send sadd("set-3", "bay") should equal(true)

      r send sunionstore("set-r", Seq("set-1", "set-2")) should equal(4)
      r send scard("set-r") should equal(4)
      r send sunionstore("set-s", Seq("set-1", "set-3")) should equal(6)
      r send scard("set-s") should equal(6)
    }
    it("should treat non-existing keys as empty sets") {
      r send sadd("set-1", "foo") should equal(true)
      r send sadd("set-1", "bar") should equal(true)
      r send sadd("set-1", "baz") should equal(true)
      r send sunionstore("set-r", Seq("set-1", "set-4")) should equal(3)
      r send scard("set-r") should equal(3)
    }
  }

  describe("sdiff") {
    it("should return diff") {
      r send sadd("set-1", "foo") should equal(true)
      r send sadd("set-1", "bar") should equal(true)
      r send sadd("set-1", "baz") should equal(true)

      r send sadd("set-2", "foo") should equal(true)
      r send sadd("set-2", "bat") should equal(true)
      r send sadd("set-2", "baz") should equal(true)

      r send sadd("set-3", "for") should equal(true)
      r send sadd("set-3", "bat") should equal(true)
      r send sadd("set-3", "bay") should equal(true)

      r send sdiff(Seq("set-1", "set-2", "set-3")) map (_.toSet) should equal(Result(Set("bar")))
    }
    it("should treat non-existing keys as empty sets") {
      r send sadd("set-1", "foo") should equal(true)
      r send sadd("set-1", "bar") should equal(true)
      r send sadd("set-1", "baz") should equal(true)
      r send sdiff(Seq("set-1", "set-2")) map (_.toSet) should equal(Result(Set("foo", "bar", "baz")))
    }
  }

  describe("smembers") {
    it("should return members of a set") {
      r send sadd("set-1", "foo") should equal(true)
      r send sadd("set-1", "bar") should equal(true)
      r send sadd("set-1", "baz") should equal(true)
      r send smembers("set-1") map (_.toSet) should equal(Result(Set("foo", "bar", "baz")))
    }
    it("should return None for an empty set") {
      r send smembers("set-1") map (_.toSet) should equal(Result(Set()))
    }
  }

  describe("srandmember") {
    it("should return a random member") {
      r send sadd("set-1", "foo") should equal(true)
      r send sadd("set-1", "bar") should equal(true)
      r send sadd("set-1", "baz") should equal(true)
      r send srandmember("set-1") should (equal(Result("foo")) or equal(Result("bar")) or equal(Result("baz")))
    }
    it("should return None for a non-existing key") {
      r send srandmember("set-1") should equal(NoResult)
    }
  }
}
