package net.fyrie.redis

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers

class HashSpec extends Spec
  with ShouldMatchers
  with RedisTestServer {

  describe("hset") {
    it("should set and get fields") {
      r.quiet.hset("hash1", "field1", "val")
      r.sync.hget("hash1", "field1").parse[String] should be(Some("val"))
    }

    it("should set and get maps") {
      r.quiet.hmset("hash2", Map("field1" -> "val1", "field2" -> "val2"))
      r.sync.hmget("hash2", Seq("field1")).parse[String] should be(List(Some("val1")))
      r.sync.hmget("hash2", Seq("field1", "field2")).parse[String] should be(List(Some("val1"), Some("val2")))
      r.sync.hmget("hash2", Seq("field1", "field2", "field3")).parse[String] should be(List(Some("val1"), Some("val2"), None))
    }

    it("should increment map values") {
      r.sync.hincrby("hash3", "field1", 1) should be(1)
      r.sync.hget("hash3", "field1").parse[String] should be(Some("1"))
    }

    it("should check existence") {
      r.quiet.hset("hash4", "field1", "val")
      r.sync.hexists("hash4", "field1") should equal(true)
      r.sync.hexists("hash4", "field2") should equal(false)
    }

    it("should delete fields") {
      r.quiet.hset("hash5", "field1", "val")
      r.sync.hexists("hash5", "field1") should equal(true)
      r.quiet.hdel("hash5", "field1")
      r.sync.hexists("hash5", "field1") should equal(false)
    }

    it("should return the length of the fields") {
      r.quiet.hmset("hash6", Map("field1" -> "val1", "field2" -> "val2"))
      r.sync.hlen("hash6") should be(2)
    }

    it("should return the aggregates") {
      r.quiet.hmset("hash7", Map("field1" -> "val1", "field2" -> "val2"))
      r.sync.hkeys("hash7").parse[String] should be(Set("field1", "field2"))
      r.sync.hvals("hash7").parse[String] should be(Set("val1", "val2"))
      r.sync.hgetall("hash7").parse[String, String] should be(Map("field1" -> "val1", "field2" -> "val2"))
    }
  }
}
