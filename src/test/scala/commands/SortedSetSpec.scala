package net.fyrie.redis

import Commands._

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith


@RunWith(classOf[JUnitRunner])
class SortedSetOperationsSpec extends Spec 
                        with ShouldMatchers
                        with RedisTestServer {

  private def add = {
    r send zadd("hackers", 1965, "yukihiro matsumoto") should be(true)
    r send zadd("hackers", 1953, "richard stallman") should be(true)
    r send zadd("hackers", 1916, "claude shannon") should be(true)
    r send zadd("hackers", 1969, "linus torvalds") should be(true)
    r send zadd("hackers", 1940, "alan kay") should be(true)
    r send zadd("hackers", 1912, "alan turing") should be(true)
  }

  describe("zadd") {
    it("should add based on proper sorted set semantics") {
      add
      r send zadd("hackers", 1912, "alan turing") should be(false)
      r send zcard("hackers") should equal(6)
    }
  }

  describe("zrange") {
    it("should get the proper range") {
      add
      r send zrange("hackers", 0, -1) getOrElse fail("No result") should have size (6)
      r send zrangeWithScores("hackers", 0, -1) getOrElse fail("No result") should have size(6)
    }
  }

  describe("zrank") {
    it ("should give proper rank") {
      add
      r send zrank("hackers", "yukihiro matsumoto") should equal(4)
      r send zrevrank("hackers", "yukihiro matsumoto") should equal(1)
    }
  }

  describe("zremrangebyrank") {
    it ("should remove based on rank range") {
      add
      r send zremrangebyrank("hackers", 0, 2) should equal(3)
    }
  }

  describe("zremrangebyscore") {
    it ("should remove based on score range") {
      add
      r send zremrangebyscore("hackers", 1912, 1940) should equal(3)
      r send zremrangebyscore("hackers", 0, 3) should equal(0)
    }
  }

  describe("zunion") {
    it ("should do a union") {
      r send zadd("hackers 1", 1965, "yukihiro matsumoto") should be(true)
      r send zadd("hackers 1", 1953, "richard stallman") should be(true)
      r send zadd("hackers 2", 1916, "claude shannon") should be(true)
      r send zadd("hackers 2", 1969, "linus torvalds") should be(true)
      r send zadd("hackers 3", 1940, "alan kay") should be(true)
      r send zadd("hackers 4", 1912, "alan turing") should be(true)

      // union with weight = 1
      r send zunionstore("hackers", Seq("hackers 1", "hackers 2", "hackers 3", "hackers 4")) should equal(6)
      r send zcard("hackers") should equal(6)

      r send zrangeWithScores("hackers", 0, -1) should equal(Some(List(("alan turing", 1912), ("claude shannon",1916), ("alan kay",1940), ("richard stallman",1953), ("yukihiro matsumoto",1965), ("linus torvalds",1969))))

      // union with modified weights
      r send zunionstoreWeighted("hackers weighted", Map(("hackers 1", 1), ("hackers 2", 2), ("hackers 3", 3), ("hackers 4", 4))) should equal(6)
      r send zrangeWithScores("hackers weighted", 0, -1) map (_.map(_._2)) should equal(Some(List(1953, 1965, 3832, 3938, 5820, 7648)))
    }
  }
}
