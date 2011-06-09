package net.fyrie.redis

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers

class SortedSetSpec extends Spec
                    with ShouldMatchers
                    with RedisTestServer {

  private def add = {
    r.sync.zadd("hackers", "yukihiro matsumoto", 1965) should be(true)
    r.sync.zadd("hackers", "richard stallman", 1953) should be(true)
    r.sync.zadd("hackers", "claude shannon", 1916) should be(true)
    r.sync.zadd("hackers", "linus torvalds", 1969) should be(true)
    r.sync.zadd("hackers", "alan kay", 1940) should be(true)
    r.sync.zadd("hackers", "alan turing", 1912) should be(true)
  }

  describe("zadd") {
    it("should add based on proper sorted set semantics") {
      add
      r.sync.zadd("hackers", "alan turing", 1912) should be(false)
      r.sync.zcard("hackers") should equal(6)
    }
  }

  describe("zrange") {
    it("should get the proper range") {
      add
      r.sync.zrange("hackers", 0, -1) should have size (6)
      r.sync.zrangeWithScores("hackers", 0, -1) should have size(6)
    }
  }

  describe("zrank") {
    it ("should give proper rank") {
      add
      r.sync.zrank("hackers", "yukihiro matsumoto") should equal(Some(4))
      r.sync.zrevrank("hackers", "yukihiro matsumoto") should equal(Some(1))
    }
  }

  describe("zremrangebyrank") {
    it ("should remove based on rank range") {
      add
      r.sync.zremrangebyrank("hackers", 0, 2) should equal(3)
    }
  }

  describe("zremrangebyscore") {
    it ("should remove based on score range") {
      add
      r.sync.zremrangebyscore("hackers", 1912, 1940) should equal(3)
      r.sync.zremrangebyscore("hackers", 0, 3) should equal(0)
    }
  }

  describe("zunion") {
    it ("should do a union") {
      r.sync.zadd("hackers 1", "yukihiro matsumoto", 1965) should be(true)
      r.sync.zadd("hackers 1", "richard stallman", 1953) should be(true)
      r.sync.zadd("hackers 2", "claude shannon", 1916) should be(true)
      r.sync.zadd("hackers 2", "linus torvalds", 1969) should be(true)
      r.sync.zadd("hackers 3", "alan kay", 1940) should be(true)
      r.sync.zadd("hackers 4", "alan turing", 1912) should be(true)

      // union with weight = 1
      r.sync.zunionstore("hackers", Set("hackers 1", "hackers 2", "hackers 3", "hackers 4")) should equal(6)
      r.sync.zcard("hackers") should equal(6)

      r.sync.zrangeWithScores("hackers").parse[String] should equal(List(("alan turing", 1912), ("claude shannon",1916), ("alan kay",1940), ("richard stallman",1953), ("yukihiro matsumoto",1965), ("linus torvalds",1969)))

      // union with modified weights
      r.sync.zunionstoreWeighted("hackers weighted", Map(("hackers 1", 1), ("hackers 2", 2), ("hackers 3", 3), ("hackers 4", 4))) should equal(6)
      r.sync.zrangeWithScores("hackers weighted").parse[String] map(_._2) should equal(List(1953, 1965, 3832, 3938, 5820, 7648))
    }
  }
}
